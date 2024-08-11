import React from 'react';
import { View, Text, YStack, XStack } from 'tamagui';
import { bgGreen, textBlack } from './colors';
import CustomButton from './components/CustomButton';


interface StartScreenProps {
    setSelectedTab: (tab: string) => void;
}
const StartScreen: React.FC<StartScreenProps> = ({ setSelectedTab }) => {
    return (
        <YStack flex={1} mt="$12" gap="$3">

            <Text color={textBlack} fontSize={50}>Generate a ZK Proof of your location using
                <Text style={{ textDecorationLine: 'underline', textDecorationColor: bgGreen }}> Zalileo</Text></Text>
            <XStack f={1} />
            <CustomButton text="Let's start" onPress={() => setSelectedTab('prove')} />
        </YStack>
    );
};

export default StartScreen;
