import { useState, useEffect, useRef, useCallback } from 'react';
import type { ChatMessage } from '../types/chat';

export const useChat = (roomId: string) => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isConnected, setIsConnected] = useState<boolean>(false);
    const [userId] = useState<string>(() => {
        const savedId = localStorage.getItem('chat-user-id');
        if (savedId) return savedId;

        const newId = `user-${Math.floor(Math.random() * 10000)}`;
        localStorage.setItem('chat-user-id', newId);
        return newId;
    });

    const wsRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        let isMounted = true;

        const initializeNetwork = async () => {
            try {
                const tokenResponse = await fetch(`/api/auth/token?userId=${userId}`);
                if (!tokenResponse.ok) throw new Error('Authentication rejected');
                const token = await tokenResponse.text();

                const historyResponse = await fetch(`/api/v1/history/${roomId}?limit=50`);
                if (historyResponse.ok) {
                    const historyData: ChatMessage[] = await historyResponse.json();
                    if (isMounted) setMessages(historyData.reverse());
                }

                const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const wsUrl = `${protocol}//${window.location.host}/chat?token=${token}`;

                const socket = new WebSocket(wsUrl);

                socket.onopen = () => {
                    if (isMounted) setIsConnected(true);

                    const joinMessage: ChatMessage = {
                        messageId: `join-${crypto.randomUUID()}`,
                        roomId,
                        senderId: userId,
                        content: 'Joined the network',
                        timestamp: new Date().toISOString(),
                        type: 'JOIN'
                    };
                    socket.send(JSON.stringify(joinMessage));
                };

                socket.onmessage = (event) => {
                    const incomingMessage: ChatMessage = JSON.parse(event.data);
                    if (isMounted) {
                        setMessages(prev => [...prev, incomingMessage]);
                    }
                };

                socket.onclose = () => {
                    if (isMounted) setIsConnected(false);
                };

                wsRef.current = socket;
            } catch (error) {
                console.error('Network initialization failed:', error);
            }
        };

        initializeNetwork();

        return () => {
            isMounted = false;
            if (wsRef.current?.readyState === WebSocket.OPEN) {
                wsRef.current.close();
            }
        };
    }, [roomId, userId]);

    const sendMessage = useCallback((content: string) => {
        if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;

        const chatMessage: ChatMessage = {
            messageId: `msg-${crypto.randomUUID()}`,
            roomId,
            senderId: userId,
            content,
            timestamp: new Date().toISOString(),
            type: 'CHAT'
        };

        wsRef.current.send(JSON.stringify(chatMessage));
    }, [roomId, userId]);

    return { messages, isConnected, sendMessage, userId };
};