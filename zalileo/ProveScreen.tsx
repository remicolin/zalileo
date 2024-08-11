import React, { useState, useEffect } from 'react';
import { YStack, Text, XStack, Slider, Spinner } from 'tamagui';
import { bgGreen, textBlack } from './colors';
import CustomButton from './components/CustomButton';

import MapView, { Marker, Circle } from 'react-native-maps';
import { Cpu } from '@tamagui/lucide-icons';

interface ProveScreenProps {
    setSelectedTab: (tab: string) => void;
    setIsInRange: (isInRange: boolean) => void;
}


const ProveScreen: React.FC<ProveScreenProps> = ({ setSelectedTab, setIsInRange }) => {
    const initialRegion = {
        latitude: 37.78825,
        longitude: -122.4324,
        latitudeDelta: 0.001, // Approximately 100m zoom
        longitudeDelta: 0.001,
    };

    const [userMarker, setUserMarker] = useState<{ latitude: number; longitude: number } | null>(null);
    const [sliderValue, setSliderValue] = useState(0);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if (userMarker) {
            const distance = calculateDistance(
                initialRegion.latitude,
                initialRegion.longitude,
                userMarker.latitude,
                userMarker.longitude
            );
            setIsInRange(distance <= sliderValue);
        }
    }, [userMarker, sliderValue]);

    const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number) => {
        const R = 6371e3; // Earth's radius in meters
        const φ1 = lat1 * Math.PI / 180;
        const φ2 = lat2 * Math.PI / 180;
        const Δφ = (lat2 - lat1) * Math.PI / 180;
        const Δλ = (lon2 - lon1) * Math.PI / 180;

        const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in meters
    };

    const handleProve = () => {
        setIsLoading(true);
        // Sleep for 5 seconds then set isLoading to false
        const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
        sleep(3500).then(() => {
            setIsLoading(false);
            setSelectedTab("verify");

        });
    }

    return (
        <YStack f={1} mt="$10" gap="$3">
            <MapView
                alignSelf="center"
                style={{ width: '100%', height: 300, borderRadius: 20 }}
                initialRegion={initialRegion}
                region={initialRegion}
                scrollEnabled={false}
                zoomEnabled={false}
                rotateEnabled={false}
                pitchEnabled={false}
                onPress={(e) => setUserMarker(e.nativeEvent.coordinate)}
            >
                <Marker
                    coordinate={initialRegion}
                    pinColor="red"
                />
                {userMarker && (
                    <>
                        <Marker
                            coordinate={userMarker}
                            pinColor="blue"
                        />
                        <Circle
                            center={userMarker}
                            radius={sliderValue}
                            fillColor="rgba(0, 0, 255, 0.1)"
                            strokeColor="rgba(0, 0, 255, 0.5)"
                        />
                    </>
                )}
            </MapView>

            <YStack px="$4" gap="$4">

                <Slider
                    alignSelf="center"
                    mt="$5"
                    size="$4"
                    width="100%"
                    defaultValue={[sliderValue]}
                    max={60}
                    step={1}
                    onValueChange={(value) => setSliderValue(value[0])}
                >
                    <Slider.Track>
                        <Slider.TrackActive />
                    </Slider.Track>
                    <Slider.Thumb circular size="$2" index={0} />
                </Slider>
                <Text fontSize="$6">Radius: {sliderValue} meters</Text>

            </YStack>
            <XStack f={1} />


            <XStack f={1} />

            <XStack f={1} />
            <CustomButton isDisabled={isLoading} Icon={isLoading ? <Spinner size="small" /> : <Cpu />} text="Generate Proof" onPress={handleProve} />
        </YStack >
    );
};

export default ProveScreen;