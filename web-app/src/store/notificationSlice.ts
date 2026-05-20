import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

interface NotificationState {
  unreadCount: number;
}

const initialState: NotificationState = {
  unreadCount: 0,
};

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setUnreadCount(state, action: PayloadAction<number>) {
      state.unreadCount = Math.max(0, action.payload);
    },
    incrementUnreadCount(state, action: PayloadAction<number | undefined>) {
      state.unreadCount += action.payload ?? 1;
      if (state.unreadCount < 0) state.unreadCount = 0;
    },
    decrementUnreadCount(state, action: PayloadAction<number | undefined>) {
      state.unreadCount -= action.payload ?? 1;
      if (state.unreadCount < 0) state.unreadCount = 0;
    },
    clearUnreadCount(state) {
      state.unreadCount = 0;
    },
  },
});

export const { setUnreadCount, incrementUnreadCount, decrementUnreadCount, clearUnreadCount } = notificationSlice.actions;
export default notificationSlice.reducer;
