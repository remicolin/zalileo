/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, { useState } from 'react';
import type { PropsWithChildren } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';

import ProveScreen from './ProveScreen';

import { Tabs, YStack } from 'tamagui';
import { bgWhite } from './colors';
import VerifyScreen from './VerifyScreen';
import StartScreen from './StartScreen';

function App(): React.JSX.Element {

  const [selectedTab, seSelectedtab] = useState("start")
  const [isInRange, setIsInRange] = useState(false);

  return (
    <YStack flex={1} bg={bgWhite} p="$4" pb="$6">
      <Tabs f={1} orientation="horizontal" flexDirection="column" defaultValue={"start"}
        value={selectedTab}
        onValueChange={(value) => seSelectedtab(value)}
      >
        <Tabs.Content value="prove" f={1}>
          <ProveScreen setSelectedTab={seSelectedtab} setIsInRange={setIsInRange} />
        </Tabs.Content>

        <Tabs.Content value="verify" f={1}>
          <VerifyScreen setSelectedTab={seSelectedtab} isInRange={isInRange} />
        </Tabs.Content>
        <Tabs.Content value="start" f={1}>
          <StartScreen setSelectedTab={seSelectedtab} />
        </Tabs.Content>


      </Tabs>

    </YStack>
  );
}


export default App;
