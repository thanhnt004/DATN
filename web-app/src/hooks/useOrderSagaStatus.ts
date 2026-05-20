import { useEffect, useState } from 'react';
import { subscribeToOrderSagaStatus } from '../api/orderSagaSse';
import type { OrderSagaEvent } from '../api/orderSagaSse';

export interface UseOrderSagaStatusResult {
  isProcessing: boolean;
  isCompleted: boolean;
  isFailed: boolean;
  currentStep: string | null;
  errorMessage: string | null;
  sagaEvent: OrderSagaEvent | null;
}

/**
 * React hook to subscribe to order saga status updates
 * 
 * @param orderId - The order ID to subscribe to (null to not subscribe)
 * @returns Saga status state
 * 
 * @example
 * ```tsx
 * function OrderCreationStatus({ orderId }: { orderId: string }) {
 *   const { isProcessing, isCompleted, isFailed, currentStep, errorMessage } = 
 *     useOrderSagaStatus(orderId);
 * 
 *   if (isProcessing) {
 *     return <div>Processing: {currentStep}...</div>;
 *   }
 * 
 *   if (isFailed) {
 *     return <div>Failed: {errorMessage}</div>;
 *   }
 * 
 *   if (isCompleted) {
 *     return <div>Order created successfully!</div>;
 *   }
 * 
 *   return null;
 * }
 * ```
 */
export function useOrderSagaStatus(orderId: string | null): UseOrderSagaStatusResult {
  const [isProcessing, setIsProcessing] = useState(false);
  const [isCompleted, setIsCompleted] = useState(false);
  const [isFailed, setIsFailed] = useState(false);
  const [currentStep, setCurrentStep] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [sagaEvent, setSagaEvent] = useState<OrderSagaEvent | null>(null);

  useEffect(() => {
    if (!orderId) {
      return;
    }

    console.log('[useOrderSagaStatus] Subscribing to order:', orderId);
    setIsProcessing(true);
    setIsCompleted(false);
    setIsFailed(false);
    setCurrentStep(null);
    setErrorMessage(null);

    const unsubscribe = subscribeToOrderSagaStatus(orderId, {
      onProcessing: (event) => {
        console.log('[useOrderSagaStatus] Processing:', event.currentStep);
        setIsProcessing(true);
        setCurrentStep(event.currentStep);
        setSagaEvent(event);
      },
      onCompleted: (event) => {
        console.log('[useOrderSagaStatus] Completed');
        setIsProcessing(false);
        setIsCompleted(true);
        setCurrentStep(null);
        setSagaEvent(event);
      },
      onFailed: (event) => {
        console.log('[useOrderSagaStatus] Failed:', event.errorMessage);
        setIsProcessing(false);
        setIsFailed(true);
        setErrorMessage(event.errorMessage || 'Unknown error');
        setSagaEvent(event);
      },
      onError: (error) => {
        console.error('[useOrderSagaStatus] Connection error:', error);
        setIsProcessing(false);
        setIsFailed(true);
        setErrorMessage('Connection error occurred');
      },
    });

    // Cleanup on unmount or when orderId changes
    return () => {
      console.log('[useOrderSagaStatus] Cleanup');
      unsubscribe();
    };
  }, [orderId]);

  return {
    isProcessing,
    isCompleted,
    isFailed,
    currentStep,
    errorMessage,
    sagaEvent,
  };
}
