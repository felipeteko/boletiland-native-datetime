import { WebPlugin } from '@capacitor/core';

import type { NativeDateTimePlugin } from './definitions';

export class NativeDateTimeWeb extends WebPlugin implements NativeDateTimePlugin {
  async syncFromRemote(): Promise<boolean> {
    console.log('syncFromRemote');
    return true;
  }

  async getCurrent(): Promise<{ datetime: string; }> {
    return {
      datetime: new Date().toLocaleString()
    };  
  }
}
