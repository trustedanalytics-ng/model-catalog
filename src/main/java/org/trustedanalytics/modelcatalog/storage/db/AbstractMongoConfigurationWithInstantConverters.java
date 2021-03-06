/**
 * Copyright (c) 2016 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.trustedanalytics.modelcatalog.storage.db;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.CustomConversions;

import java.time.Instant;
import java.util.Arrays;

public abstract class AbstractMongoConfigurationWithInstantConverters extends
        AbstractMongoConfiguration {

  @Override
  public CustomConversions customConversions() {
    return new CustomConversions(Arrays.asList(new InstantToLongConverter(), new
            LongToInstantConverter()));
  }

  private static class InstantToLongConverter implements Converter<Instant, Long> {
    @Override
    public Long convert(Instant instant) {
      return instant.toEpochMilli();
    }
  }

  private static class LongToInstantConverter implements Converter<Long, Instant> {
    @Override
    public Instant convert(Long source) {
      return Instant.ofEpochMilli(source);
    }
  }

}
