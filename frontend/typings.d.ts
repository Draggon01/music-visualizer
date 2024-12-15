declare module 'sockjs-client' {
    class SockJS {
        constructor(url: string, protocols?: string | string[], options?: any);
        close(code?: number, reason?: string): void;
        send(data: string): void;
        onopen: () => void;
        onmessage: (e: { data: string }) => void;
        onclose: (e: { code: number; reason: string; wasClean: boolean }) => void;
    }
    export default SockJS;
}