export interface NativeDateTimePlugin {
  syncFromRemote(): Promise<boolean>;
  getCurrent(): Promise<{ datetime: string }>;
}
