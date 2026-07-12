// Realtime client seam. The dashboard only ever talks to `RealtimeClient`;
// `createRealtimeClient()` picks the STOMP implementation (real backend) or the
// in-browser fake feed (mock mode) — flipping VITE_USE_MOCK is the only change.

import { Client } from '@stomp/stompjs';
import { USE_MOCK, WS_URL } from './config';
import { session } from './auth';
import { FakeFeed } from './mock/fake-feed';
import type { Topic, TopicPayloads } from './types/events';

export type Unsubscribe = () => void;

export interface RealtimeClient {
  connect(): void;
  disconnect(): void;
  on<T extends Topic>(topic: T, cb: (event: TopicPayloads[T]) => void): Unsubscribe;
}

type AnyCb = (event: unknown) => void;

/** Shared listener registry used by both implementations. */
export class Listeners {
  private map = new Map<Topic, Set<AnyCb>>();

  add(topic: Topic, cb: AnyCb): Unsubscribe {
    let set = this.map.get(topic);
    if (!set) {
      set = new Set();
      this.map.set(topic, set);
    }
    set.add(cb);
    return () => set.delete(cb);
  }

  emit<T extends Topic>(topic: T, event: TopicPayloads[T]): void {
    this.map.get(topic)?.forEach((cb) => cb(event));
  }
}

class StompRealtimeClient implements RealtimeClient {
  private listeners = new Listeners();
  private client: Client;

  constructor() {
    this.client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 2000,
      connectHeaders: session.accessToken
        ? { Authorization: `Bearer ${session.accessToken}` }
        : {},
      onConnect: () => {
        const topics: Topic[] = ['vehicle-position', 'vehicle-status', 'vehicle-health', 'trip-status', 'alerts', 'kpi'];
        for (const topic of topics) {
          this.client.subscribe(`/topic/${topic}`, (msg) => {
            this.listeners.emit(topic, JSON.parse(msg.body));
          });
        }
      },
    });
  }

  connect(): void {
    this.client.activate();
  }

  disconnect(): void {
    void this.client.deactivate();
  }

  on<T extends Topic>(topic: T, cb: (event: TopicPayloads[T]) => void): Unsubscribe {
    return this.listeners.add(topic, cb as AnyCb);
  }
}

let instance: RealtimeClient | null = null;

export function realtime(): RealtimeClient {
  if (!instance) {
    instance = USE_MOCK ? new FakeFeed() : new StompRealtimeClient();
  }
  return instance;
}
