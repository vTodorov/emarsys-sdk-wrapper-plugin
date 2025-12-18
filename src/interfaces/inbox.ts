interface InboxMessageBaseAction {

  id: string;
  title: string;

  /**
   * @since 1.0.0
   */
  type: string;
}

export interface InboxMessageAppAction extends InboxMessageBaseAction {
  /**
   * @since 1.0.0
   */
  name: string;

  /**
   * @since 1.0.0
   */
  payload: { [key: string]: any };
}

export interface InboxMessageCustomAction extends InboxMessageBaseAction {
  /**
   * @since 1.0.0
   */
  name: string;

  /**
   * @since 1.0.0
   */
  payload: { [key: string]: any };
}

export interface InboxMessageExternalUrlAction extends InboxMessageBaseAction {
  /**
   * @since 1.0.0
   */
  url: string;
}

export type InboxMessageAction = InboxMessageAppAction | InboxMessageCustomAction | InboxMessageExternalUrlAction;

export interface InboxMessage {
  /**
   * @since 1.0.0
   */
  id: string;

  /**
   * @since 1.0.0
   */
  campaignId: string;

  /**
   * @since 1.0.0
   */
  collapseId?: string;

  /**
   * @since 1.0.0
   */
  title: string;

  /**
   * @since 1.0.0
   */
  body: string;

  /**
   * @since 1.0.0
   */
  imageUrl?: string;

  /**
   * @since 1.0.0
   */
  receivedAt: number;

  /**
   * @since 1.0.0
   */
  updatedAt?: number;

  /**
   * @since 1.0.0
   */
  expiresAt?: number;

  /**
   * @since 1.0.0
   */
  tags: string[];

  /**
   * @since 1.0.0
   */
  properties: { [key: string]: string };

  /**
   * @since 1.0.0
   */
  actions: InboxMessageAction[];
}

export interface InboxTag {
  /**
   * @since 1.0.0
   */
  tag: string;

  /**
   * @since 1.0.0
   */
  messageId: string;
}
