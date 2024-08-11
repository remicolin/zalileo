import React from 'react';
import { YStack, Button, Image, Text, styled, XStack } from 'tamagui';
import { ArrowRight, Camera, CheckCircle, CheckCircle2, Cpu, SquarePen, UserPlus } from '@tamagui/lucide-icons';
import { bgColor, bgGreen, borderColor, greenColorDark, greenColorLight, textBlack, textColor1, textColor2 } from './colors';
import CustomButton from './components/CustomButton';


interface VerifyScreenProps {
    setSelectedTab: (tab: string) => void;
}

const VerifyScreen: React.FC<VerifyScreenProps> = ({ setSelectedTab }) => {

    return (
        <YStack f={1} mt="$8" gap="$6">
            <YStack gap="$3">
                <Text ml="$4" color={textBlack} fontSize="$9" >ZK Verify</Text>
                <YStack bg="white" borderRadius="$8" p="$4" gap="$2">
                    <CheckCircle2 alignSelf="center" size="$10" color={greenColorLight} />
                    <YStack gap="$2" mb="$3">
                        <Text color={textBlack} fontSize="$5" >start proof verification:</Text>
                        <Text color={textBlack} fontSize="$5" >proof verification:</Text>
                    </YStack>
                    <CustomButton Icon={<CheckCircle2 />} text="Verify" onPress={() => { }} />
                </YStack>
            </YStack>
            <YStack gap="$3">
                <Text ml="$4" color={textBlack} fontSize="$9" >ZK Verify</Text>
                <YStack bg="white" borderRadius="$8" p="$4" gap="$2">
                    <CheckCircle2 alignSelf="center" size="$10" color={greenColorLight} />
                    <YStack gap="$2" mb="$3">
                        <Text color={textBlack} fontSize="$5" >start proof verification:</Text>
                        <Text color={textBlack} fontSize="$5" >proof verification:</Text>
                    </YStack>
                    <CustomButton Icon={<CheckCircle2 />} text="Verify" onPress={() => { }} />
                </YStack>
            </YStack>
            <XStack f={1} />
            <CustomButton text="Return to prove" onPress={() => { setSelectedTab("prove") }} />
        </YStack>
    );
};

export default VerifyScreen;
