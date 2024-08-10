import React from 'react';
import { YStack, Button, Image, Text, styled } from 'tamagui';
import { ArrowRight, Camera, SquarePen, UserPlus } from '@tamagui/lucide-icons';
import { bgColor, bgGreen, borderColor, textBlack, textColor1, textColor2 } from './colors';
import CustomButton from './components/CustomButton';

const ProveScreen: React.FC = () => {

    return (
        <YStack f={1} p="$3">
            <YStack f={1} mt="$12">
                <Text fontSize="$9" >Welcome to Zalileo 👋</Text>
            </YStack>
        </YStack >
    );
};

export default ProveScreen;
