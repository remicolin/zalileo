use risc0_zkvm::guest::env;
use p256::{
    ecdsa::{signature::Verifier, Signature, VerifyingKey},
    EncodedPoint,
};
use anyhow::{Context, Result};
use bitvec::{field::BitField, vec::BitVec};
use clap::Parser;
use galileo_osnma::{
    galmon::{navmon::{nav_mon_message::{GalileoInav, Type}, NavMonMessage}, transport::{ReadTransport, WriteTransport}},
    storage::FullStorage,
    types::{BitSlice, NUM_SVNS},
    Gst, InavBand, Osnma, PublicKey, Svn, Validated, Wn,
};
use spki::DecodePublicKey;
use std::io::Read;
use chrono::NaiveDateTime;
use std::{fs, path::PathBuf};
use nmea_parser::*;
use sha2::{Sha256, Digest};

fn load_pubkey(hex: &str, pkid: u8) -> Result<PublicKey<Validated>> {
    let pubkey = hex::decode(hex)?;
    let pubkey = p256::ecdsa::VerifyingKey::from_sec1_bytes(&pubkey)?;
    Ok(PublicKey::from_p256(pubkey, pkid).force_valid())
}

fn main() {
    env_logger::init();
    let (pubkey, file, signature): (String, String, Signature) = env::read();
    
    println!("Signature: {:?}", signature);

    let verifying_key = p256::ecdsa::VerifyingKey::from_sec1_bytes(&hex::decode(&pubkey).unwrap()).unwrap();
    println!("Verifying key: {:?}", verifying_key);

    let mut hasher = Sha256::new();
    hasher.update(&file.as_bytes());
    let message = hasher.finalize();

    println!("Guest Message: {:?}", message);

    // Verify the signature, panicking if verification fails.
    verifying_key
        .verify(&message, &signature)
        .expect("ECDSA signature verification failed");

    let mut parser = NmeaParser::new();
    let sentences = file.split("\n");
    // Parse the sentences and print some fields of the messages
    for sentence in sentences {   
        if sentence.starts_with("#") {
            continue;
        } 
        match parser.parse_sentence(sentence).unwrap_or(ParsedMessage::Incomplete) {
            ParsedMessage::VesselDynamicData(vdd) => {
                println!("MMSI:    {}",        vdd.mmsi);
                println!("Speed:   {:.1} kts", vdd.sog_knots.unwrap());
                println!("Heading: {}°",       vdd.heading_true.unwrap());
                println!("");
            },
            ParsedMessage::VesselStaticData(vsd) => {
                println!("MMSI:  {}", vsd.mmsi);
                println!("Flag:  {}", vsd.country().unwrap());
                println!("Name:  {}", vsd.name.unwrap());
                println!("Type:  {}", vsd.ship_type);
                println!("");
            },
            ParsedMessage::Gga(gga) => {
                println!("Source:    {}",     gga.source);
                println!("Latitude:  {:.3}°", gga.latitude.unwrap());
                println!("Longitude: {:.3}°", gga.longitude.unwrap());
                println!("");
            },
            ParsedMessage::Rmc(rmc) => {
                println!("Source:  {}",        rmc.source);
                println!("Speed:   {:.1} kts", rmc.sog_knots.unwrap());
                println!("Bearing: {}°",       rmc.bearing.unwrap());
                println!("Time:    {}",        rmc.timestamp.unwrap());
                println!("");
            },
            _ => {
            }
        }
    }

    /*let buffer = Vec::new();
    let packets = &input[..];
    let mut read = ReadTransport::new(packets);
    let mut write = WriteTransport::new(buffer);
    let mut total_size = 0;
    // There should be 17 packets in the test data
    for _ in 0..17 {
        let packet = read.read_packet().unwrap().unwrap();
        total_size += write.write_packet(&packet).unwrap();
    }
    assert_eq!(total_size, packets.len());
    println!("Successfully read and wrote {} bytes", total_size);*/

    /*let pubkey = "0474a925cfa0ff1805e5c5a58fdba31bf0145d5b5be2f062d3f8bb2ee98f0f6db081a85d5d40af893fedd2b3c6149c149219f0dc129f580a94583bc9591377d2a9";
    //let pubkey_pem = String::from("-----BEGIN PUBLIC KEY-----MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdKklz6D/GAXlxaWP26Mb8BRdW1vi8GLT+Lsu6Y8PbbCBqF1dQK+JP+3Ss8YUnBSSGfDcEp9YCpRYO8lZE3fSqQ==-----END PUBLIC KEY-----");
    let pkid = 1;
    let pubkey = load_pubkey(pubkey, pkid);
    //let merkle_root = "0E63F552C8021709043C239032EFFE941BF22C8389032F5F2701E0FBC80148B8";

    println!("Loaded public key");
    println!("Pubkey: {:?}", pubkey);

    let mut osnma: Osnma<FullStorage> = Osnma::from_pubkey(pubkey.unwrap(), true);

    let mut read = ReadTransport::new(input.as_slice());
    let mut timing_parameters: [Option<[u8; 18]>; NUM_SVNS] = [None; NUM_SVNS];
    let mut ced_and_status_data: [Option<[u8; 69]>; NUM_SVNS] = [None; NUM_SVNS];
    let mut current_subframe = None;
    let mut last_tow_mod_30 = 0;

    while let Ok(Some(packet)) = read.read_packet() {
        if let Some(
            inav @ GalileoInav {
                contents: inav_word,
                reserved1: osnma_data,
                sigid: Some(sigid),
                ..
            },
        ) = &packet.gi
        {
            // This is needed because sometimes we can see a TOW of 604801
            let secs_in_week = 604800;
            let mut tow = inav.gnss_tow % secs_in_week;
            let wn = Wn::try_from(inav.gnss_wn).unwrap()
                + Wn::try_from(inav.gnss_tow / secs_in_week).unwrap();

            // Fix bug in Galmon data:
            //
            // Often, the E1B word 16 starting at TOW = 29 mod 30 will have the
            // TOW of the previous word 16 in the subframe, which starts at TOW
            // = 15 mod 30. We detect this condition by looking at the last tow
            // mod 30 that we saw and fixing if needed.
            if tow % 30 == 15 && last_tow_mod_30 >= 19 {
                log::debug!(
                    "fixing wrong TOW for SVN {}; tow = {}, last tow mod 30 = {}",
                    inav.gnss_sv,
                    tow,
                    last_tow_mod_30
                );
                tow += 29 - 15; // wn rollover is not possible by this addition
            }
            last_tow_mod_30 = tow % 30;

            let gst = Gst::new(wn, tow);
            if let Some(current) = current_subframe {
                if current > gst.gst_subframe() {
                    // Avoid processing INAV words that are in a previous subframe
                    log::warn!(
                        "dropping INAV word from previous subframe (current subframe {:?}, \
			 this INAV word {:?} SVN {} band {})",
                        current,
                        gst,
                        inav.gnss_sv,
                        sigid
                    );
                    continue;
                }
            }
            current_subframe = Some(gst.gst_subframe());
            let svn = Svn::try_from(inav.gnss_sv).unwrap();
            let band = match sigid {
                1 => InavBand::E1B,
                5 => InavBand::E5B,
                _ => {
                    log::error!("INAV word received on non-INAV band: sigid = {}", sigid);
                    continue;
                }
            };

            // The OSNMA SIS ICD says that OSNMA is not provided in INAV Dummy
            // Messages or Alert Pages. The OSNMA field in these pages may not
            // contain all zeros, but is invalid and should be discarded.
            //
            // Here we drop INAV words that are Dummy Messages. There is no way
            // for us to filter for Alert Pages in Galmon data (the page type
            // bit is not present), so hopefully these pages don't make it here.
            let inav_word_type = inav_word[0] >> 2;
            if inav_word_type == 63 {
                log::debug!(
                    "discarding dummy INAV word from {} {:?} at {:?}",
                    svn,
                    band,
                    gst
                );
                continue;
            }

            osnma.feed_inav(inav_word[..].try_into().unwrap(), svn, gst, band);
            if let Some(osnma_data) = osnma_data {
                osnma.feed_osnma(osnma_data[..].try_into().unwrap(), svn, gst);
            }

            for svn in Svn::iter() {
                let idx = usize::from(svn) - 1;
                if let Some(data) = osnma.get_ced_and_status(svn) {
                    let mut data_bytes = [0u8; 69];
                    let a = BitSlice::from_slice_mut(&mut data_bytes);
                    let b = data.data();
                    a[..b.len()].copy_from_bitslice(b);
                    if !ced_and_status_data[idx]
                        .map(|d| d == data_bytes)
                        .unwrap_or(false)
                    {
                        log::info!(
                            "new CED and status for {} authenticated \
                                    (authbits = {}, GST = {:?})",
                            svn,
                            data.authbits(),
                            data.gst()
                        );
                        ced_and_status_data[idx] = Some(data_bytes);
                    }
                }
                if let Some(data) = osnma.get_timing_parameters(svn) {
                    let mut data_bytes = [0u8; 18];
                    let a = BitSlice::from_slice_mut(&mut data_bytes);
                    let b = data.data();
                    a[..b.len()].copy_from_bitslice(b);
                    if !timing_parameters[idx]
                        .map(|d| d == data_bytes)
                        .unwrap_or(false)
                    {
                        log::info!(
                            "new timing parameters for {} authenticated (authbits = {}, GST = {:?})",
			    svn,
                            data.authbits(),
                            data.gst()
			);
                        timing_parameters[idx] = Some(data_bytes);
                    }
                }
            }
        }
    }*/

    env::commit(&(pubkey, file));
}
