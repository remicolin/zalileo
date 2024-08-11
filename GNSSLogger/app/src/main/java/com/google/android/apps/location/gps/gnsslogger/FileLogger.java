/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.location.gps.gnsslogger;

import android.content.Context;
import android.content.Intent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.google.android.apps.location.gps.gnsslogger.LoggerFragment.UIFragmentComponent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.math.BigInteger;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;



/** A GNSS logger to store information to a file. */
public class FileLogger implements MeasurementListener {

  private static final String TAG = "FileLogger";
  private static final String FILE_PREFIX = "gnss_log";
  private static final String ERROR_WRITING_FILE = "Problem writing to file.";
  private static final String COMMENT_START = "# ";
  private static final char RECORD_DELIMITER = ',';
  private static final String VERSION_TAG = "Version: ";

  private static final int MAX_FILES_STORED = 100;
  private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

  private final Context mContext;

  private final Object mFileLock = new Object();
  private BufferedWriter mFileWriter;
  private File mFile;

  private UIFragmentComponent mUiComponent;

  public synchronized UIFragmentComponent getUiComponent() {
    return mUiComponent;
  }

  public synchronized void setUiComponent(UIFragmentComponent value) {
    mUiComponent = value;
  }

  public FileLogger(Context context) {
    this.mContext = context;
  }
  public static PrivateKey loadPrivateKey() throws Exception {
    KeyPairGenerator kg = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec kpgparams = new ECGenParameterSpec("secp256r1");
    kg.initialize(kpgparams);

    KeyPair kp = kg.generateKeyPair();
    PublicKey pubKey = kp.getPublic();
    //dump the public key in a file
    File publicKeyFile = new File("/storage/emulated/0/Download/publicKey.txt");
    BufferedWriter publicKeyFileWriter = new BufferedWriter(new FileWriter(publicKeyFile));
    publicKeyFileWriter.write(Base64.getEncoder().encodeToString(pubKey.getEncoded()));
    publicKeyFileWriter.flush();
    publicKeyFileWriter.close();

    PrivateKey pvtKey = kp.getPrivate();

    //dump the private key in a file
    File privateKeyFile = new File("/storage/emulated/0/Download/privateKey.txt");
    BufferedWriter privateKeyFileWriter = new BufferedWriter(new FileWriter(privateKeyFile));
    privateKeyFileWriter.write(Base64.getEncoder().encodeToString(pvtKey.getEncoded()));
    privateKeyFileWriter.flush();
    privateKeyFileWriter.close();
    return pvtKey;
  }
  public byte[] signFile(File file, PrivateKey privateKey) throws Exception {
    Signature signature = Signature.getInstance("SHA256withECDSA");  // ECDSA with SHA-256
    signature.initSign(privateKey);

    byte[] fileBytes = new byte[0];
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      fileBytes = Files.readAllBytes(file.toPath());
    }
    signature.update(fileBytes);

    return signature.sign();
  }

  /**
   * Start a new file logging process.
   */
  public void startNewLog() {
    synchronized (mFileLock) {
      File baseDirectory;
      String state = Environment.getExternalStorageState();
      if (Environment.MEDIA_MOUNTED.equals(state)) {
        //baseDirectory = mContext.getExternalFilesDir(null);
        baseDirectory = new File("/storage/emulated/0/Download/");
        //create a file in the download directory
        //baseDirectory.mkdirs();
      } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
        logError("Cannot write to external storage.");
        return;
      } else {
        logError("Cannot read external storage.");
        return;
      }

      SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
      Date now = new Date();
      String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(now));
      File currentFile = new File(baseDirectory, fileName);
      String currentFilePath = currentFile.getAbsolutePath();
      BufferedWriter currentFileWriter;
      try {
        currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
      } catch (IOException e) {
        logException("Could not open file: " + currentFilePath, e);
        return;
      }

      // initialize the contents of the file
      try {
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.write("Header Description:");
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.write(VERSION_TAG);
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String fileVersion =
                mContext.getString(R.string.app_version)
                        + " Platform: "
                        + Build.VERSION.RELEASE
                        + " "
                        + "Manufacturer: "
                        + manufacturer
                        + " "
                        + "Model: "
                        + model;
        currentFileWriter.write(fileVersion);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.write(
                "Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                        + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                        + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                        + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                        + "PseudorangeRateUncertaintyMetersPerSecond,"
                        + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                        + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                        + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                        + "ConstellationType,AgcDb");
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.write(
                "Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs");
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.write("Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)");
        currentFileWriter.newLine();
        currentFileWriter.write(COMMENT_START);
        currentFileWriter.newLine();
      } catch (IOException e) {
        logException("Count not initialize file: " + currentFilePath, e);
        return;
      }

      if (mFileWriter != null) {
        try {
          mFileWriter.close();
        } catch (IOException e) {
          logException("Unable to close all file streams.", e);
          return;
        }
      }

      mFile = currentFile;
      mFileWriter = currentFileWriter;
      Log.d(TAG, "File opened: " + currentFilePath);
      Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Send the current log via email or other options selected from a pop menu shown to the user. A
   * new log is started when calling this function.
   */
  public void send() {
    if (mFile == null) {
      return;
    }
    Log.d(TAG, "TRYING TO SEEEEEEEEEND");
//    Intent emailIntent = new Intent(Intent.ACTION_SEND);
//    emailIntent.setType("*/*");
//    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SensorLog");
//    emailIntent.putExtra(Intent.EXTRA_TEXT, "");
//    // attach the file
//    Uri fileURI =
//        FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", mFile);
//    emailIntent.putExtra(Intent.EXTRA_STREAM, fileURI);
//    getUiComponent().startActivity(Intent.createChooser(emailIntent, "Send log.."));
    if (mFileWriter != null) {
      Log.d(TAG, "Closing file");
      try {
        mFileWriter.flush();
        mFileWriter.close();
        mFileWriter = null;
      } catch (IOException e) {
        logException("Unable to close all file streams.", e);
        return;
      }
    }


    try {
      Log.d(TAG, "outputing Signing file");
      PrivateKey privateKey = loadPrivateKey();
      byte[] signature = signFile(mFile, privateKey);
      //dump the signature in a file
      File signatureFile = new File("/storage/emulated/0/Download/signature.txt");
      BufferedWriter signatureFileWriter = new BufferedWriter(new FileWriter(signatureFile));
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Log.d(TAG, "Signature: " + Base64.getEncoder().encodeToString(signature));
        signatureFileWriter.write(Base64.getEncoder().encodeToString(signature));
        Toast.makeText(mContext, "Outputing the signature", Toast.LENGTH_SHORT).show();
      }
      else {

        Toast.makeText(mContext, "Couldnt make it", Toast.LENGTH_SHORT).show();
      }
      signatureFileWriter.flush();
      signatureFileWriter.close();
    } catch (Exception e) {
      Toast.makeText(mContext, "Error signing file"+e, Toast.LENGTH_LONG).show();
      try {
        File errorFile = new File("/storage/emulated/0/Download/error.txt");
        BufferedWriter errorFileWriter;
        errorFileWriter = new BufferedWriter(new FileWriter(errorFile));
        errorFileWriter.write(e.toString());
        errorFileWriter.flush();
        errorFileWriter.close();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }

      Log.d(TAG, "Error signing file", e);
      Log.e(TAG, "Error signing file", e);
    }
  }


  @Override
  public void onProviderEnabled(String provider) {}

  @Override
  public void onProviderDisabled(String provider) {}

  @Override
  public void onLocationChanged(Location location) {
    synchronized (mFileLock) {
      if (mFileWriter == null) {
        return;
      }
      String locationStream =
          String.format(
              Locale.US,
              "Fix,%s,%f,%f,%f,%f,%f,%d",
              location.getProvider(),
              location.getLatitude(),
              location.getLongitude(),
              location.getAltitude(),
              location.getSpeed(),
              location.getAccuracy(),
              location.getTime());
      try {
        mFileWriter.write(locationStream);
        mFileWriter.newLine();
      } catch (IOException e) {
        logException(ERROR_WRITING_FILE, e);
      }
    }
  }

  @Override
  public void onLocationStatusChanged(String provider, int status, Bundle extras) {}

  @Override
  public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
    synchronized (mFileLock) {
      if (mFileWriter == null) {
        logException("FileWriter not working", null);
        return;
      }
      Log.d("FileLogger", "Writing to file");
      GnssClock gnssClock = event.getClock();
      for (GnssMeasurement measurement : event.getMeasurements()) {
        try {
          writeGnssMeasurementToFile(gnssClock, measurement);
        } catch (IOException e) {
          logException(ERROR_WRITING_FILE, e);
        }
      }
    }
  }

  @Override
  public void onGnssMeasurementsStatusChanged(int status) {}

  @Override
  public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
    synchronized (mFileLock) {
      if (mFileWriter == null) {
        return;
      }
      StringBuilder builder = new StringBuilder("Nav");
      builder.append(RECORD_DELIMITER);
      builder.append(navigationMessage.getSvid());
      builder.append(RECORD_DELIMITER);
      builder.append(navigationMessage.getType());
      builder.append(RECORD_DELIMITER);

      int status = navigationMessage.getStatus();
      builder.append(status);
      builder.append(RECORD_DELIMITER);
      builder.append(navigationMessage.getMessageId());
      builder.append(RECORD_DELIMITER);
      builder.append(navigationMessage.getSubmessageId());
      byte[] data = navigationMessage.getData();
      for (byte word : data) {
        builder.append(RECORD_DELIMITER);
        builder.append(word);
      }
      try {
        mFileWriter.write(builder.toString());
        mFileWriter.newLine();
      } catch (IOException e) {
        logException(ERROR_WRITING_FILE, e);
      }
    }
  }

  @Override
  public void onGnssNavigationMessageStatusChanged(int status) {}

  @Override
  public void onGnssStatusChanged(GnssStatus gnssStatus) {}

  @Override
  public void onNmeaReceived(long timestamp, String s) {
    synchronized (mFileLock) {
      if (mFileWriter == null) {
        return;
      }
      String nmeaStream = String.format(Locale.US, "NMEA,%s,%d", s.trim(), timestamp);
      try {
        mFileWriter.write(nmeaStream);
        mFileWriter.newLine();
      } catch (IOException e) {
        logException(ERROR_WRITING_FILE, e);
      }
    }
  }

  @Override
  public void onListenerRegistration(String listener, boolean result) {}

  @Override
  public void onTTFFReceived(long l) {}

  private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurement measurement)
      throws IOException {
    String clockStream =
        String.format(
            "Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            SystemClock.elapsedRealtime(),
            clock.getTimeNanos(),
            clock.hasLeapSecond() ? clock.getLeapSecond() : "",
            clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
            clock.getFullBiasNanos(),
            clock.hasBiasNanos() ? clock.getBiasNanos() : "",
            clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
            clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
            clock.hasDriftUncertaintyNanosPerSecond()
                ? clock.getDriftUncertaintyNanosPerSecond()
                : "",
            clock.getHardwareClockDiscontinuityCount() + ",");
    mFileWriter.write(clockStream);

    String measurementStream =
        String.format(
            "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            measurement.getSvid(),
            measurement.getTimeOffsetNanos(),
            measurement.getState(),
            measurement.getReceivedSvTimeNanos(),
            measurement.getReceivedSvTimeUncertaintyNanos(),
            measurement.getCn0DbHz(),
            measurement.getPseudorangeRateMetersPerSecond(),
            measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
            measurement.getAccumulatedDeltaRangeState(),
            measurement.getAccumulatedDeltaRangeMeters(),
            measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
            measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
            measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
            measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
            measurement.hasCarrierPhaseUncertainty()
                ? measurement.getCarrierPhaseUncertainty()
                : "",
            measurement.getMultipathIndicator(),
            measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
            measurement.getConstellationType(),
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && measurement.hasAutomaticGainControlLevelDb()
                ? measurement.getAutomaticGainControlLevelDb()
                : "");
    mFileWriter.write(measurementStream);
    mFileWriter.newLine();
  }

  private void logException(String errorMessage, Exception e) {
    Log.e(MeasurementProvider.TAG + TAG, errorMessage, e);
    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
  }

  private void logError(String errorMessage) {
    Log.e(MeasurementProvider.TAG + TAG, errorMessage);
    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
  }

  /**
   * Implements a {@link FileFilter} to delete files that are not in the {@link
   * FileToDeleteFilter#mRetainedFiles}.
   */
  private static class FileToDeleteFilter implements FileFilter {
    private final List<File> mRetainedFiles;

    public FileToDeleteFilter(File... retainedFiles) {
      this.mRetainedFiles = Arrays.asList(retainedFiles);
    }

    /**
     * Returns {@code true} to delete the file, and {@code false} to keep the file.
     *
     * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
     */
    @Override
    public boolean accept(File pathname) {
      if (pathname == null || !pathname.exists()) {
        return false;
      }
      if (mRetainedFiles.contains(pathname)) {
        return false;
      }
      return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
    }
  }
}
