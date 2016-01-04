/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.metadata;

import java.util.Collections;
import javax.measure.unit.SI;
import org.opengis.util.RecordType;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.PositionalAccuracy;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.metadata.iso.quality.DefaultAbsoluteExternalPositionalAccuracy;
import org.apache.sis.util.iso.DefaultRecordSchema;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Static;
import org.apache.sis.util.iso.DefaultRecord;


/**
 * Creates a record reporting coordinate transformation accuracy.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class TransformationAccuracy extends Static {
    /**
     * The name for the transformation accuracy metadata.
     */
    private static final InternationalString TRANSFORMATION_ACCURACY =
            Vocabulary.formatInternational(Vocabulary.Keys.TransformationAccuracy);

    /**
     * The type of record instances which will hold coordinate transformation accuracy values.
     */
    private static final RecordType TYPE;
    static {
        final DefaultRecordSchema schema = new DefaultRecordSchema(null, null, Constants.SIS);
        TYPE = schema.createRecordType("Real", Collections.<CharSequence,Class<?>>singletonMap(
                Vocabulary.formatInternational(Vocabulary.Keys.Value), Double.class));
    }

    /**
     * Do not allow instantiation of this class.
     */
    private TransformationAccuracy() {
    }

    /**
     * Creates a positional accuracy for the given value, in metres.
     *
     * @param accuracy The accuracy in metres.
     * @return A positional accuracy with the given value.
     */
    public static PositionalAccuracy create(final Double accuracy) {
        final DefaultRecord record = new DefaultRecord(TYPE);
        record.setAll(accuracy);

        final DefaultQuantitativeResult result = new DefaultQuantitativeResult();
        result.setValues(Collections.singletonList(record));
        result.setValueUnit(SI.METRE);              // In metres by definition in the EPSG database.
        result.setValueType(TYPE);

        final DefaultAbsoluteExternalPositionalAccuracy element =
                new DefaultAbsoluteExternalPositionalAccuracy(result);
        element.setNamesOfMeasure(Collections.singleton(TRANSFORMATION_ACCURACY));
        element.setEvaluationMethodType(EvaluationMethodType.DIRECT_EXTERNAL);
        element.freeze();
        return element;
    }
}