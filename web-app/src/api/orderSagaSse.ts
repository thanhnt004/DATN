/**
 * SSE (Server-Sent Events) utility for subscribing to order saga status updates
 * 
 * Usage:
 * ```typescript
 * const unsubscribe = subscribeToOrderSagaStatus(orderId, {
 *   onProcessing: (event) => console.log('Processing:', event.currentStep),
 *   onCompleted: (event) => console.log('Order created successfully!'),
 *   onFailed: (event) => console.error('Order creation failed:', event.errorMessage),
 *   onError: (error) => console.error('Connection error:', error),
 * });
 * 
 * // Later, to cleanup:
 * unsubscribe();
 * ```
 */

export interface OrderSagaEvent {
  orderId: string;
  orderNumber: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'COMPENSATING' | 'COMPENSATED';
  currentStep: string;
  message: string;
  errorMessage?: string;
  timestamp: string;
}

export interface SagaStatusCallbacks {
  onProcessing?: (event: OrderSagaEvent) => void;
  onCompleted?: (event: OrderSagaEvent) => void;
  onFailed?: (event: OrderSagaEvent) => void;
  onCompensating?: (event: OrderSagaEvent) => void;
  onCompensated?: (event: OrderSagaEvent) => void;
  onError?: (error: Event) => void;
}

/**
 * Subscribe to order saga status updates via SSE
 * 
 * @param orderId - The order ID to subscribe to
 * @param callbacks - Callback functions for different saga statuses
 * @returns Unsubscribe function to close the connection
 */
export function subscribeToOrderSagaStatus(
  orderId: string,
  callbacks: SagaStatusCallbacks
): () => void {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  const url = `${apiBaseUrl}/api/v1/orders/${orderId}/saga-status`;

  const eventSource = new EventSource(url);

  // Handle saga events
  eventSource.addEventListener('saga-event', (event: MessageEvent) => {
    try {
      const sagaEvent: OrderSagaEvent = JSON.parse(event.data);
      
      console.log('[SSE] Received saga event:', sagaEvent);

      // Route to appropriate callback based on status
      switch (sagaEvent.status) {
        case 'PROCESSING':
          callbacks.onProcessing?.(sagaEvent);
          break;
        case 'COMPLETED':
          callbacks.onCompleted?.(sagaEvent);
          eventSource.close(); // Auto-close on completion
          break;
        case 'FAILED':
          callbacks.onFailed?.(sagaEvent);
          eventSource.close(); // Auto-close on failure
          break;
        case 'COMPENSATING':
          callbacks.onCompensating?.(sagaEvent);
          break;
        case 'COMPENSATED':
          callbacks.onCompensated?.(sagaEvent);
          eventSource.close(); // Auto-close after compensation
          break;
      }
    } catch (error) {
      console.error('[SSE] Failed to parse saga event:', error);
    }
  });

  // Handle connection errors
  eventSource.onerror = (error) => {
    console.error('[SSE] Connection error:', error);
    callbacks.onError?.(error);
    eventSource.close();
  };

  // Return unsubscribe function
  return () => {
    console.log('[SSE] Unsubscribing from order saga status');
    eventSource.close();
  };
}
