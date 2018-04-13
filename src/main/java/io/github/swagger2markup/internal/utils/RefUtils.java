/*
 * Copyright 2018 Roberto Cortez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.swagger2markup.internal.utils;

import io.swagger.v3.parser.models.RefFormat;

public class RefUtils {
    public static RefFormat computeRefFormat(String ref) {
        return io.swagger.v3.parser.util.RefUtils.computeRefFormat(ref);
    }

    public static String computeSimpleRef(String ref) {
        String result = ref;
        //simple refs really only apply to internal refs

        if (computeRefFormat(ref).equals(RefFormat.INTERNAL)) {
            result = ref.substring(ref.lastIndexOf("/") + 1);
        }

        return result;
    }
}
