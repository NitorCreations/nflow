
class Cache<T> {
    private cache = new Map<string, {ts: number, value: T}>();
    private maxAgeMillis: number;

    constructor(maxAgeMillis: number) {
        this.maxAgeMillis = maxAgeMillis;
    }

    public setAndReturn(key: string, value: T): T {
        const now = new Date().getTime();
        this.cache.set(key, {ts: now, value: value});
        return value;
    }

    public get(key: string): T | undefined {
        const now = new Date().getTime();
        const entry = this.cache.get(key);
        if (!entry) {
            return undefined;
        }
        if ((now - entry.ts) < this.maxAgeMillis) {
            return entry.value;
        }
        return undefined;
    }
};

export { Cache };
