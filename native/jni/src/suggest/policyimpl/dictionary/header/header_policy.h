/*
 * Copyright (C) 2013, The Android Open Source Project
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

#ifndef LATINIME_HEADER_POLICY_H
#define LATINIME_HEADER_POLICY_H

#include <cctype>
#include <stdint.h>

#include "defines.h"
#include "suggest/core/policy/dictionary_header_structure_policy.h"
#include "suggest/policyimpl/dictionary/header/header_reading_utils.h"

namespace latinime {

class HeaderPolicy : public DictionaryHeaderStructurePolicy {
 public:
    explicit HeaderPolicy(const uint8_t *const dictBuf)
            : mDictBuf(dictBuf), mDictionaryFlags(HeaderReadingUtils::getFlags(dictBuf)),
              mSize(HeaderReadingUtils::getHeaderSize(dictBuf)),
              mAttributeMap(createAttributeMapAndReadAllAttributes(mDictBuf)),
              mMultiWordCostMultiplier(readMultipleWordCostMultiplier()),
              mUsesForgettingCurve(readUsesForgettingCurveFlag()),
              mLastUpdatedTime(readLastUpdatedTime()) {}

    ~HeaderPolicy() {}

    AK_FORCE_INLINE int getSize() const {
        return mSize;
    }

    AK_FORCE_INLINE bool supportsDynamicUpdate() const {
        return HeaderReadingUtils::supportsDynamicUpdate(mDictionaryFlags);
    }

    AK_FORCE_INLINE bool requiresGermanUmlautProcessing() const {
        return HeaderReadingUtils::requiresGermanUmlautProcessing(mDictionaryFlags);
    }

    AK_FORCE_INLINE bool requiresFrenchLigatureProcessing() const {
        return HeaderReadingUtils::requiresFrenchLigatureProcessing(
                mDictionaryFlags);
    }

    AK_FORCE_INLINE float getMultiWordCostMultiplier() const {
        return mMultiWordCostMultiplier;
    }

    AK_FORCE_INLINE bool usesForgettingCurve() const {
        return mUsesForgettingCurve;
    }

    AK_FORCE_INLINE int getLastUpdatedTime() const {
        return mLastUpdatedTime;
    }

    void readHeaderValueOrQuestionMark(const char *const key,
            int *outValue, int outValueSize) const;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(HeaderPolicy);

    static const char *const MULTIPLE_WORDS_DEMOTION_RATE_KEY;
    static const char *const USES_FORGETTING_CURVE_KEY;
    static const char *const LAST_UPDATED_TIME_KEY;
    static const float DEFAULT_MULTIPLE_WORD_COST_MULTIPLIER;
    static const float MULTIPLE_WORD_COST_MULTIPLIER_SCALE;

    const uint8_t *const mDictBuf;
    const HeaderReadingUtils::DictionaryFlags mDictionaryFlags;
    const int mSize;
    HeaderReadingUtils::AttributeMap mAttributeMap;
    const float mMultiWordCostMultiplier;
    const bool mUsesForgettingCurve;
    const int mLastUpdatedTime;

    float readMultipleWordCostMultiplier() const;

    bool readUsesForgettingCurveFlag() const;

    int readLastUpdatedTime() const;

    bool getAttributeValueAsInt(const char *const key, int *const outValue) const;

    static HeaderReadingUtils::AttributeMap createAttributeMapAndReadAllAttributes(
            const uint8_t *const dictBuf);

    static int parseIntAttributeValue(const std::vector<int> *const attributeValue);

    static void insertCharactersIntoVector(
            const char *const characters, std::vector<int> *const vector);
};
} // namespace latinime
#endif /* LATINIME_HEADER_POLICY_H */
