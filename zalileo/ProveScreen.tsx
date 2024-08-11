import React, { useState } from 'react';
import { YStack, Text, XStack, Slider, Spinner } from 'tamagui';
import { bgGreen, textBlack } from './colors';
import CustomButton from './components/CustomButton';

import MapView, { Marker, Circle } from 'react-native-maps';
import { Cpu } from '@tamagui/lucide-icons';

interface ProveScreenProps {
    setSelectedTab: (tab: string) => void;
}


const ProveScreen: React.FC<ProveScreenProps> = ({ setSelectedTab }) => {
    const initialRegion = {
        latitude: 37.78825,
        longitude: -122.4324,
        latitudeDelta: 0.001, // Approximately 100m zoom
        longitudeDelta: 0.001,
    };

    const [userMarker, setUserMarker] = useState<{ latitude: number; longitude: number } | null>(null);
    const [sliderValue, setSliderValue] = useState(0);
    const [isLoading, setIsLoading] = useState(false);

    const handleProve = () => {
        setIsLoading(true);
        // Sleep for 5 seconds then set isLoading to false
        const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
        sleep(5000).then(() => {
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
            {isLoading && <Spinner size="large" />}
            <XStack />
            <CustomButton Icon={<Cpu />} text="Generate Proof" onPress={handleProve} />
        </YStack >
    );
};

export default ProveScreen;