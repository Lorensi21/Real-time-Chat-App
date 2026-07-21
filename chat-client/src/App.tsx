import React, { useState, useRef, useEffect } from 'react';
import { useChat } from './hooks/useChat';

export default function App() {
    const targetRoom = 'architecture-room';
    const { messages, isConnected, sendMessage, userId } = useChat(targetRoom);

    const [input, setInput] = useState('');
    const endOfMessagesRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        endOfMessagesRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleBroadcast = (e: React.FormEvent) => {
        e.preventDefault();
        if (!input.trim()) return;

        sendMessage(input);
        setInput('');
    };

    return (
        <div style={{ maxWidth: '600px', margin: '0 auto', fontFamily: 'system-ui' }}>
            <header style={{ padding: '20px', borderBottom: '1px solid #ccc' }}>
                <h2>Distributed Chat Node</h2>
                <p>Status: {isConnected ? '🟢 Connected' : '🔴 Disconnected'} | User: {userId}</p>
            </header>

            <main style={{ height: '60vh', overflowY: 'auto', padding: '20px', backgroundColor: '#f9f9f9' }}>
                {messages.map((msg) => {
                    // Check against the string literals
                    const isSystem = msg.type === 'JOIN' || msg.type === 'LEAVE';
                    const isSelf = msg.senderId === userId;

                    if (isSystem) {
                        return (
                            <div key={msg.messageId} style={{ textAlign: 'center', color: '#888', margin: '10px 0', fontSize: '0.9em' }}>
                                <em>{msg.senderId} {msg.content}</em>
                            </div>
                        );
                    }

                    return (
                        <div key={msg.messageId} style={{ textAlign: isSelf ? 'right' : 'left', margin: '10px 0' }}>
                            <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '4px' }}>
                                {msg.senderId} • {new Date(msg.timestamp).toLocaleTimeString()}
                            </div>
                            <div style={{
                                display: 'inline-block',
                                padding: '10px 15px',
                                borderRadius: '15px',
                                backgroundColor: isSelf ? '#007bff' : '#e1e1e1',
                                color: isSelf ? 'white' : 'black'
                            }}>
                                {msg.content}
                            </div>
                        </div>
                    );
                })}
                <div ref={endOfMessagesRef} />
            </main>

            <footer style={{ padding: '20px', borderTop: '1px solid #ccc' }}>
                <form onSubmit={handleBroadcast} style={{ display: 'flex', gap: '10px' }}>
                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        placeholder="Broadcast a message..."
                        disabled={!isConnected}
                        style={{ flex: 1, padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}
                    />
                    <button type="submit" disabled={!isConnected} style={{ padding: '10px 20px', cursor: 'pointer' }}>
                        Send
                    </button>
                </form>
            </footer>
        </div>
    );
}
