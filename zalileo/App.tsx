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

function App(): React.JSX.Element {

  const [selectedTab, seSelectedtab] = useState("prove")

  return (
    <YStack flex={1} bg={bgWhite} p="$4" pb="$6">
      <Tabs f={1} orientation="horizontal" flexDirection="column" defaultValue={"prove"}
        value={selectedTab}
        onValueChange={(value) => seSelectedtab(value)}
      >
        <Tabs.Content value="prove" f={1}>
          <ProveScreen setSelectedTab={seSelectedtab} />
        </Tabs.Content>

        <Tabs.Content value="verify" f={1}>
          <VerifyScreen setSelectedTab={seSelectedtab} />
        </Tabs.Content>


      </Tabs>

    </YStack>
  );
}


export default App;
