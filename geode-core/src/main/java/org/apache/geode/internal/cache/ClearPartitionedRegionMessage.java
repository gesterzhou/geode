/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

import org.apache.geode.CancelException;
import org.apache.geode.DataSerializer;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.MessageWithReply;
import org.apache.geode.distributed.internal.PooledDistributionMessage;
import org.apache.geode.distributed.internal.ReplyException;
import org.apache.geode.distributed.internal.ReplyMessage;
import org.apache.geode.distributed.internal.ReplyProcessor21;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.serialization.DeserializationContext;
import org.apache.geode.internal.serialization.SerializationContext;

public class ClearPartitionedRegionMessage extends PooledDistributionMessage
    implements MessageWithReply {

  private int processorId;
  private String regionPath;
  private EventID eventID;

  public ClearPartitionedRegionMessage() {}

  public ClearPartitionedRegionMessage(RegionEventImpl event, ReplyProcessor21 replyProcessor,
      Set<InternalDistributedMember> recipients) {
    this.setRecipients(recipients);
    this.processorId = replyProcessor.getProcessorId();
    this.eventID = event.getEventId();
    this.regionPath = event.getRegion().getFullPath();
  }

  protected static void send(RegionEventImpl event, Set<InternalDistributedMember> recipients) {
    DistributionManager dm = ((PartitionedRegion) event.getRegion()).getDistributionManager();
    ReplyProcessor21 replyProcessor = new ReplyProcessor21(dm, recipients);
    ClearPartitionedRegionMessage msg =
        new ClearPartitionedRegionMessage(event, replyProcessor, recipients);
    dm.putOutgoing(msg);
    try {
      replyProcessor.waitForReplies();
    } catch (ReplyException e) {
      if (!(e.getCause() instanceof CancelException)) {
        throw e;
      }
    } catch (InterruptedException e) {
      dm.getCancelCriterion().checkCancelInProgress(e);
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public int getDSFID() {
    return CLEAR_PARTITIONED_REGION_MESSAGE;
  }

  @Override
  protected void process(ClusterDistributionManager dm) {
    PartitionedRegion region =
        (PartitionedRegion) dm.getCache().getRegion(this.regionPath);
    if (region != null) {
      System.out
          .println(Thread.currentThread().getName()
              + ": ClearPartitionedRegionMessage.process region=" + region.getFullPath());
      region.clearLocalPrimaryBuckets();
    }
    ReplyMessage.send(getSender(), this.processorId, true, dm);
  }

  @Override
  public void fromData(DataInput in, DeserializationContext context)
      throws IOException, ClassNotFoundException {
    super.fromData(in, context);
    this.processorId = DataSerializer.readPrimitiveInt(in);
    this.regionPath = DataSerializer.readString(in);
    this.eventID = DataSerializer.readObject(in);
  }

  @Override
  public void toData(DataOutput out, SerializationContext context) throws IOException {
    super.toData(out, context);
    DataSerializer.writePrimitiveInt(this.processorId, out);
    DataSerializer.writeString(this.regionPath, out);
    DataSerializer.writeObject(this.eventID, out);
  }
}
