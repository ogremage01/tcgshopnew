import { create } from "zustand";

export type AdminAlarmSseStatus =
  | "idle"
  | "connecting"
  | "connected"
  | "reconnecting"
  | "error";

interface AdminAlarmSseStore {
  status: AdminAlarmSseStatus;
  connectionId: number | null;
  connectedAt: number | null;
  lastEventAt: number | null;
  lastHeartbeatAt: number | null;
  alarmCount: number;
  errorMessage: string | null;
  setConnecting: (connectionId: number) => void;
  setConnected: () => void;
  setHeartbeat: () => void;
  setAlarmReceived: () => void;
  setReconnecting: () => void;
  setError: (message: string) => void;
  reset: () => void;
}

const initialState = {
  status: "idle" as AdminAlarmSseStatus,
  connectionId: null,
  connectedAt: null,
  lastEventAt: null,
  lastHeartbeatAt: null,
  alarmCount: 0,
  errorMessage: null,
};

export const useAdminAlarmSseStore = create<AdminAlarmSseStore>()((set) => ({
  ...initialState,
  setConnecting: (connectionId) =>
    set((state) => ({
      connectionId,
      errorMessage: null,
      status:
        state.status === "connected" ? "reconnecting" : "connecting",
    })),
  setConnected: () =>
    set({
      status: "connected",
      connectedAt: Date.now(),
      lastEventAt: Date.now(),
      errorMessage: null,
    }),
  setHeartbeat: () =>
    set({
      lastHeartbeatAt: Date.now(),
      lastEventAt: Date.now(),
    }),
  setAlarmReceived: () =>
    set((state) => ({
      lastEventAt: Date.now(),
      alarmCount: state.alarmCount + 1,
    })),
  setReconnecting: () =>
    set({
      status: "reconnecting",
    }),
  setError: (message) =>
    set({
      status: "error",
      errorMessage: message,
    }),
  reset: () => set(initialState),
}));
