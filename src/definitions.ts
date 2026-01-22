/// <reference types="@capacitor/cli" />

import type { PluginListenerHandle, PermissionState } from '@capacitor/core';

import type { SetContactOptions } from './interfaces/base';
import type { PushMessageEvent, TokenResult } from './interfaces/push';
import type { ITokenInitializationStatus, PushMessageDTO, UserInformationDTO } from './interfaces/pushAndroid';

type ConsoleLogLevels = 'trace' | 'debug' | 'info' | 'warn' | 'error' | 'basic';

declare module '@capacitor/cli' {
  export interface PluginsConfig {
    EmarsysSDKCustom?: {
      mobileEngageApplicationCode?: string;
      merchantId?: string;
      consoleLogLevels?: ConsoleLogLevels[];
    };
  }
}

export interface CartItem {
  item: string;
  quantity: number;
  price: number;
}

export interface EmarsysSDKCustomPlugin {
  /**
   * Echo test method
   */
  echo(options: { value: string }): Promise<{ value: string }>;

  /**
   * Get device UUID
   */
  getUUID(value: string): Promise<{ value: string }>;

  /**
   * Request push notification permissions
   */
  requestPermissions(): Promise<PermissionState>;

  /**
   * Check push notification permissions
   */
  checkPermissions(): Promise<PermissionState>;

  /**
   * Set Emarsys contact
   */
  setContact(options: SetContactOptions): Promise<void>;

  /**
   * Clear Emarsys contact
   */
  clearContact(): Promise<void>;

  /**
   * Get push token
   */
  getPushToken(): Promise<TokenResult>;

  /**
   * Register for push notifications
   */
  register(): Promise<TokenResult>;

  /**
   * Track custom event
   */
  trackEvent(options?: { eventName: string; eventAttributes: any }): Promise<{ value: string }>;

  /**
   * Track cart event
   */
  trackCart(items?: { items: CartItem[] }): Promise<{ value: string }>;

  /**
   * Track trackItemView
   */
  trackItemView(options: { itemId?: string }): Promise<void>;

  /**
   * Android: set Firebase push token
   */
  setPushTokenFirebase(data: { value: string }): Promise<ITokenInitializationStatus>;

  /**
   * Android: set push message
   */
  setPushMessage(data: PushMessageDTO): Promise<{ value: PushMessageDTO }>;

  /**
   * Android: get user information
   */
  getUserInfo(data: UserInformationDTO): Promise<{ value: unknown }>;

  /**
   * Get device information
   */
  getDeviceInformation(options?: { value?: string }): Promise<{ value: string }>;

  /**
   * Load inline in-app message
   */
  loadInlineInapp(data: { inAppName: string }): Promise<void>;

  /**
   * Listen to Emarsys events
   */
  addListener(
    eventName:
      | 'pushMessageEvent'
      | 'EmarsysInAppDeepLink'
      | 'EmarsysInAppApplicationEvent'
      | 'EmarsysPushDeepLink'
      | 'EmarsysPushApplicationEvent'
      | 'EmarsysPushNotificationReceived',
    listenerFunc: (event: PushMessageEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}
