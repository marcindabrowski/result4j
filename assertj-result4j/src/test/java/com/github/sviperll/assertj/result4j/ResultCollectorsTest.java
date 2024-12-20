/*
 * #%L
 * %%
 * Copyright (C) 2024 The result4j Contributors (https://github.com/sviperll/result4j)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.github.sviperll.assertj.result4j;

import com.github.sviperll.result4j.AdaptingCatcher;
import com.github.sviperll.result4j.Catcher;
import com.github.sviperll.result4j.Result;
import com.github.sviperll.result4j.ResultCollectors;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static com.github.sviperll.assertj.result4j.ResultAssert.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

public class ResultCollectorsTest {
    @Test
    public void nonAdaptedRuntimeExceptionFailure() {
        Catcher.ForFunctions<NumberFormatException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions();
        Result<List<Integer>, NumberFormatException> result =
                Stream.of("123", "234", "xvxv", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(result)
                .isError()
                .hasErrorThat()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isExactlyInstanceOf(NumberFormatException.class)
                .hasMessage("For input string: \"xvxv\"");
    }

    @Test
    public void nonAdaptedRuntimeExceptionSuccess() {
        Catcher.ForFunctions<NumberFormatException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions();
        Result<List<Integer>, NumberFormatException> result =
                Stream.of("123", "234", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(result)
                .isSuccess()
                .hasSuccessValueThat()
                .asInstanceOf(list(Integer.class))
                .containsExactlyInAnyOrderElementsOf(List.of(456, 234, 123));
    }

    @Test
    public void adaptedRuntimeExceptionFailure() {
        AdaptingCatcher.ForFunctions<NumberFormatException, RuntimeException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions().map(RuntimeException::new);
        Result<List<Integer>, RuntimeException> result =
                Stream.of("123", "234", "xvxv", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(result)
                .isError()
                .hasErrorThat()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isExactlyInstanceOf(RuntimeException.class)
                .cause()
                .isExactlyInstanceOf(NumberFormatException.class)
                .hasMessage("For input string: \"xvxv\"");
    }

    @Test
    public void adaptedRuntimeExceptionSuccess() {
        AdaptingCatcher.ForFunctions<NumberFormatException, RuntimeException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions().map(RuntimeException::new);
        Result<List<Integer>, RuntimeException> result =
                Stream.of("123", "234", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(result)
                .isSuccess()
                .hasSuccessValueThat()
                .asInstanceOf(list(Integer.class))
                .containsExactlyInAnyOrderElementsOf(List.of(123, 456, 234));
    }

    @Test
    public void multipleAdaptedCheckedExceptionsSuccess() {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        Result<List<Animal>, PipelineException> animals =
                Stream.of("cat.jpg", "dog.jpg")
                        .map(io.catching(TestSystem::readFile))
                        .map(Result.flatMapping(ml.catching(TestModel::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(animals)
                .isSuccess()
                .hasSuccessValueThat()
                .asInstanceOf(list(Animal.class))
                .containsExactlyInAnyOrderElementsOf(List.of(Animal.CAT, Animal.DOG));
    }

    @Test
    public void multipleAdaptedCheckedExceptionsFailure1() {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        Result<List<Animal>, PipelineException> animals =
                Stream.of("cat.jpg", "dog.jpg", "non-existent.jpg")
                        .map(io.catching(TestSystem::readFile))
                        .map(Result.flatMapping(ml.catching(TestModel::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));


        assertThat(animals)
                .isError()
                .hasErrorThat()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isExactlyInstanceOf(PipelineException.class)
                .cause()
                .isInstanceOf(IOException.class)
                .hasMessage("non-existent.jpg: not found");
    }

    @Test
    public void multipleAdaptedCheckedExceptionsFailure2() {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        Result<List<Animal>, PipelineException> animals =
                Stream.of("cat.jpg", "dog.jpg", "corrupted.jpg")
                        .map(io.catching(TestSystem::readFile))
                        .map(Result.flatMapping(ml.catching(TestModel::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));

        assertThat(animals)
                .isError()
                .hasErrorThat()
                .asInstanceOf(InstanceOfAssertFactories.THROWABLE)
                .isExactlyInstanceOf(PipelineException.class)
                .cause()
                .isExactlyInstanceOf(MLException.class);
    }

    enum Animal {
        DOG, CAT
    }

    static class TestSystem {
        static String readFile(String name) throws IOException {
            return switch (name) {
                case "cat.jpg" -> "cat-image";
                case "dog.jpg" -> "dog-image";
                case "corrupted.jpg" -> "corrupted";
                default -> throw new FileNotFoundException("%s: not found".formatted(name));
            };
        }
    }

    static class TestModel {
        static Animal recognizeImage(String imageData) throws MLException {
            return switch (imageData) {
                case "cat-image" -> Animal.CAT;
                case "dog-image" -> Animal.DOG;
                default -> throw new MLException();
            };
        }
    }

    @SuppressWarnings("serial")
    static class MLException extends Exception {
    }

    @SuppressWarnings("serial")
    static class PipelineException extends Exception {
        PipelineException(IOException ex) {
            super(ex);
        }

        PipelineException(MLException ex) {
            super(ex);
        }
    }
}
