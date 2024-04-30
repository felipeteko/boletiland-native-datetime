import { registerPlugin } from '@capacitor/core';

import type { NativeDateTimePlugin } from './definitions';

const NativeDateTime = registerPlugin<NativeDateTimePlugin>('NativeDateTime', {
  web: () => import('./web').then(m => new m.NativeDateTimeWeb()),
});

export * from './definitions';
export { NativeDateTime };
