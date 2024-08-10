/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
// import ProveScreen from './Prove';
import { name as appName } from './app.json';
// import { TamaguiProvider, createTamagui } from 'tamagui';
// import { config } from '@tamagui/config/v2-native'
// const tamaguiConfig = createTamagui(config)


const Root = () => (
    <App />
);

AppRegistry.registerComponent(appName, () => Root);
