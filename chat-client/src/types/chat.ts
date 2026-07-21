export type MessageType = 'CHAT' | 'JOIN' | 'LEAVE';

export interface ChatMessage {
    messageId: string;
    roomId: string;
    senderId: string;
    content: string;
    timestamp: string;
    type: MessageType;
}