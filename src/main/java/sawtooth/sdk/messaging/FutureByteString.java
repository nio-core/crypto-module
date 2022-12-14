/* Copyright 2016, 2017 Intel Corporation
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

package sawtooth.sdk.messaging;

import com.google.protobuf.ByteString;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A future that resolves to ByteString.
 */
public class FutureByteString implements Future {
  /**
   * The result ByteString that is to be resolved.
   */
  private ByteString result;

  /**
   * The coorelation id associated with the message being waited for.
   */
  private String correlationId;

  /**
   * Lock to make the actions on this object synchronous.
   */
  private final ReentrantLock lock;

  /**
   * A condition variable to wait for the result.
   */
  private final Condition condition;

  /**
   * Constructor.
   * @param id created with Stream.generateId, to match future with it's result
   */
  public FutureByteString(final String id) {
    this.lock = new ReentrantLock();
    this.condition = lock.newCondition();
    this.correlationId = id;
    this.result = null;
  }

  /**
   * Returns the ByteString result, waiting for it to not be null.
   * @return ByteString protobuf
   * @throws InterruptedException an interrupt happens during the method call.
   */
  public final ByteString getResult() throws InterruptedException {
    ByteString byteString = null;
    lock.lock();
    try {
      if (result == null) {
        condition.await();
      }
      byteString = result;
    } finally {
      lock.unlock();
    }

    return byteString;
  }

  /**
   * Returns the ByteString result. If the timeout expires, throws
   * TimeoutException.
   * @param timeout time to wait for a result.
   * @return ByteString protobuf
   * @throws InterruptedException an interrupt happens during the method call.
   * @throws TimeoutException     the result is not received before the timeout.
   */
  public final ByteString getResult(final long timeout) throws InterruptedException, TimeoutException {
    ByteString byteString = null;
    lock.lock();
    try {
      if (result == null) {
        condition.await(timeout, TimeUnit.SECONDS);
      }
      byteString = result;
    } finally {
      lock.unlock();
    }
    if (byteString == null) {
      throw new TimeoutException("Future Timed out");
    }
    return byteString;
  }

  /**
   * Call this method to set the result.
   * @param byteString the byteString used to resolve the future
   */
  public final void setResult(final ByteString byteString) {
    lock.lock();
    try {
      result = byteString;
      condition.signalAll();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Returns true if the future has a result, otherwise false.
   * @return answer boolean
   */
  public final boolean isDone() {
    boolean answer = false;
    lock.lock();
    try {
      answer = result != null;
    } finally {
      lock.unlock();
    }
    return answer;
  }

  /**
   * Get the value of the coorelation id.
   * @return String coorelation id.
   */
  public final String getCorrelationId() {
    return this.correlationId;
  }

}
