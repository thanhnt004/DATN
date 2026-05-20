/**
 * Example Component: Order Creation with Real-time Status Updates
 * 
 * This demonstrates how to:
 * 1. Create an order
 * 2. Subscribe to saga status updates via SSE
 * 3. Show processing state, success, or error messages
 */

import { useState } from 'react';
import { createOrder } from '../api/orderApi';
import { useOrderSagaStatus } from '../hooks/useOrderSagaStatus';
import type { CreateOrderRequest } from '../types/order';

export function OrderCreationExample() {
  const [orderId, setOrderId] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  
  // Subscribe to saga status updates
  const { isProcessing, isCompleted, isFailed, currentStep, errorMessage } = 
    useOrderSagaStatus(orderId);

  const handleCreateOrder = async () => {
    setIsCreating(true);
    
    try {
      // Example order data
      const orderData: CreateOrderRequest = {
        sellerId: 'some-seller-id',
        paymentMethod: 'COD',
        recipientName: 'John Doe',
        recipientPhone: '0123456789',
        shippingAddress: '123 Main St',
        shippingWard: 'Ward 1',
        shippingDistrict: 'District 1',
        shippingCity: 'Ho Chi Minh',
        items: [
          {
            skuId: 'sku-123',
            productId: 'product-123',
            productName: 'Sample Product',
            skuCode: 'SKU-001',
            imageUrl: 'https://example.com/image.jpg',
            unitPrice: 100000,
            quantity: 2,
            variantInfo: { Color: "Red", Size: "M" },
          },
        ],
        shippingFee: 30000,
        discountAmount: 0,
      };

      const response = await createOrder(orderData);
      
      if (response.data.success) {
        // Order created, now subscribe to saga updates
        setOrderId(response.data.result?.[0]?.id ?? null);
      }
    } catch (error) {
      console.error('Failed to create order:', error);
      setIsCreating(false);
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div className="order-creation-example">
      <h2>Tạo Đơn Hàng</h2>
      
      {/* Create Order Button */}
      {!orderId && (
        <button 
          onClick={handleCreateOrder} 
          disabled={isCreating}
        >
          {isCreating ? 'Đang tạo đơn hàng...' : 'Đặt Hàng'}
        </button>
      )}

      {/* Processing Status */}
      {isProcessing && (
        <div className="processing-status">
          <div className="spinner"></div>
          <p>Đơn hàng đang được xử lý...</p>
          {currentStep && <p className="step-info">{currentStep}</p>}
        </div>
      )}

      {/* Success Status */}
      {isCompleted && (
        <div className="success-status">
          <h3>✅ Đặt hàng thành công!</h3>
          <p>Đơn hàng của bạn đã được tạo và đang chờ người bán xác nhận.</p>
          <button onClick={() => {
            // Navigate to order detail page
            window.location.href = `/orders/${orderId}`;
          }}>
            Xem Chi Tiết Đơn Hàng
          </button>
        </div>
      )}

      {/* Error Status */}
      {isFailed && (
        <div className="error-status">
          <h3>❌ Đặt hàng thất bại</h3>
          <p className="error-message">{errorMessage}</p>
          <button onClick={() => {
            setOrderId(null);
            setIsCreating(false);
          }}>
            Thử Lại
          </button>
        </div>
      )}
    </div>
  );
}

/**
 * Alternative Component: Simpler inline usage without the hook
 */
export function OrderCreationInlineExample() {
  const [status, setStatus] = useState<'idle' | 'processing' | 'completed' | 'failed'>('idle');
  const [error, setError] = useState<string>('');

  const handleCreateOrder = async () => {
    setStatus('processing');

    try {
      const orderData: CreateOrderRequest = {
        sellerId: 'some-seller-id',
        paymentMethod: 'COD',
        recipientName: 'John Doe',
        recipientPhone: '0123456789',
        shippingAddress: '123 Main St',
        shippingWard: 'Ward 1',
        shippingDistrict: 'District 1',
        shippingCity: 'Ho Chi Minh',
        items: [
          {
            skuId: 'sku-123',
            productId: 'product-123',
            productName: 'Sample Product',
            skuCode: 'SKU-001',
            imageUrl: 'https://example.com/image.jpg',
            unitPrice: 100000,
            quantity: 2,
            variantInfo: { Color: 'Red', Size: 'M' },
          },
        ],
        shippingFee: 30000,
        discountAmount: 0,
      };

      const response = await createOrder(orderData);
      if (response.data.success && response.data.result?.length) {
        setStatus('completed');
      } else {
        setStatus('failed');
        setError('Không thể tạo đơn hàng');
      }
    } catch {
      setStatus('failed');
      setError('Failed to create order');
    }
  };

  return (
    <div>
      {status === 'idle' && <button onClick={handleCreateOrder}>Đặt Hàng</button>}
      {status === 'processing' && <div>Đang xử lý...</div>}
      {status === 'completed' && <div>✅ Thành công!</div>}
      {status === 'failed' && <div>❌ Lỗi: {error}</div>}
    </div>
  );
}
