import React, { useState } from 'react';
import { YStack, Text, XStack, Spinner } from 'tamagui';
import { CheckCircle2, ChevronLeft, XCircle } from '@tamagui/lucide-icons';
import { bgGreen, bgWhite, greenColorLight, redColorLight, textBlack } from './colors';
import CustomButton from './components/CustomButton';

enum VerificationStep {
    Waiting,
    Connecting,
    Connected,
    Formatting,
    Sending,
    Verifying
}

const stepMessages: Record<VerificationStep, string> = {
    [VerificationStep.Waiting]: "start proof verification",
    [VerificationStep.Connecting]: "connecting to websocket...",
    [VerificationStep.Connected]: "websocket connected",
    [VerificationStep.Formatting]: "formatting proof...",
    [VerificationStep.Sending]: "proof sent",
    [VerificationStep.Verifying]: "proof verified"
};

interface VerificationProcessProps {
    isInRange: boolean;
}

const VerificationProcess: React.FC<VerificationProcessProps> = ({ isInRange }) => {
    const [step, setStep] = useState<VerificationStep>(VerificationStep.Waiting);
    const [isLoading, setIsLoading] = useState(false);

    const handleVerify = async () => {
        setIsLoading(true);
        setStep(VerificationStep.Connecting);
        await new Promise(resolve => setTimeout(resolve, 800));
        setStep(VerificationStep.Connected);
        await new Promise(resolve => setTimeout(resolve, 400));
        setStep(VerificationStep.Formatting);
        await new Promise(resolve => setTimeout(resolve, 400));
        setStep(VerificationStep.Sending);
        await new Promise(resolve => setTimeout(resolve, 800));
        setStep(VerificationStep.Verifying);
        setIsLoading(false);
    }

    const renderStatus = () => {
        if (step === VerificationStep.Waiting) return null;
        if (step !== VerificationStep.Verifying) return null;
        return isInRange ? (
            <CheckCircle2 alignSelf="center" size="$10" color={greenColorLight} />
        ) : (
            <XCircle alignSelf="center" size="$10" color={redColorLight} />
        );
    }

    return (
        <YStack bg="white" borderRadius="$8" p="$4" gap="$2" h={240}>
            <XStack f={1} />
            {renderStatus()}
            <YStack gap="$2" mb="$3">
                <Text textAlign='center' color={textBlack} fontSize="$5">{stepMessages[step]}</Text>
            </YStack>
            <CustomButton

                isDisabled={isLoading}
                bgColor={isLoading ? bgWhite : bgGreen}
                Icon={isLoading ? <Spinner /> : <CheckCircle2 />}
                text="Verify"
                onPress={handleVerify}
            />
        </YStack>
    );
};

interface VerifyScreenProps {
    setSelectedTab: (tab: string) => void;
    isInRange: boolean;
}

const VerifyScreen: React.FC<VerifyScreenProps> = ({ setSelectedTab, isInRange }) => {
    return (
        <YStack f={1} mt="$8" gap="$7">
            <XStack onPress={() => setSelectedTab("prove")}><ChevronLeft size="$2.5" /></XStack>
            <YStack gap="$3">
                <Text ml="$4" color={textBlack} fontSize="$9">ZK Verify</Text>
                <VerificationProcess isInRange={isInRange} />
            </YStack>
            <YStack gap="$3">
                <Text ml="$4" color={textBlack} fontSize="$9">Aligned</Text>
                <VerificationProcess isInRange={isInRange} />
            </YStack>
            <XStack f={1} />
        </YStack>
    );
};

export default VerifyScreen;