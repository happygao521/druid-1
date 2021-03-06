/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.server.coordinator.helper;

import com.metamx.emitter.EmittingLogger;
import io.druid.client.indexing.IndexingServiceClient;
import io.druid.segment.IndexIO;
import io.druid.server.coordinator.DatasourceWhitelist;
import io.druid.server.coordinator.DruidCoordinatorRuntimeParams;
import io.druid.timeline.DataSegment;

import java.util.concurrent.atomic.AtomicReference;

public class DruidCoordinatorVersionConverter implements DruidCoordinatorHelper
{
  private static final EmittingLogger log = new EmittingLogger(DruidCoordinatorVersionConverter.class);


  private final IndexingServiceClient indexingServiceClient;
  private final AtomicReference<DatasourceWhitelist> whitelistRef;

  public DruidCoordinatorVersionConverter(
      IndexingServiceClient indexingServiceClient,
      AtomicReference<DatasourceWhitelist> whitelistRef
  )
  {
    this.indexingServiceClient = indexingServiceClient;
    this.whitelistRef = whitelistRef;
  }

  @Override
  public DruidCoordinatorRuntimeParams run(DruidCoordinatorRuntimeParams params)
  {
    DatasourceWhitelist whitelist = whitelistRef.get();

    for (DataSegment dataSegment : params.getAvailableSegments()) {
      if (whitelist == null || whitelist.contains(dataSegment.getDataSource())) {
        final Integer binaryVersion = dataSegment.getBinaryVersion();

        if (binaryVersion == null || binaryVersion < IndexIO.CURRENT_VERSION_ID) {
          log.info("Upgrading version on segment[%s]", dataSegment.getIdentifier());
          indexingServiceClient.upgradeSegment(dataSegment);
        }
      }
    }

    return params;
  }
}
