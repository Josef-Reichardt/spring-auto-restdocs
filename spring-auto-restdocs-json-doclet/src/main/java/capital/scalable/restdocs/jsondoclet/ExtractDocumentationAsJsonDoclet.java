/*-
 * #%L
 * Spring Auto REST Docs Json Doclet
 * %%
 * Copyright (C) 2015 - 2018 Scalable Capital GmbH
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
package capital.scalable.restdocs.jsondoclet;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;

/**
 * Javadoc to JSON doclet.
 */
public class ExtractDocumentationAsJsonDoclet extends StandardDoclet {

	@Override
	public boolean run(DocletEnvironment docEnv) {

		// TODO:
		// String destinationDir = DocPaths.SOURCE_OUTPUT
		// .resolve("../generated-javadoc-json").getPath();
		try {
			String destinationDir = Files.createTempDirectory("generated-javadoc-json")
					.toString();
			ObjectMapper mapper = createObjectMapper();

			docEnv.getElementUtils().getAllModuleElements()
					.forEach(module -> module.getEnclosedElements().forEach(
							pkg -> pkg.getEnclosedElements().forEach(classOrInterface -> {
								writeToFile(destinationDir, mapper, (PackageElement) pkg,
										(TypeElement) classOrInterface, ClassDocumentation
												.fromClassDoc(docEnv, classOrInterface));
							})));

		}
		catch (IOException e) {
			throw new RuntimeException("error while creating temp dir: " + e.getMessage(),
					e);
		}
		return true;
	}

	private static void writeToFile(String destinationDir, ObjectMapper mapper,
			PackageElement packageElement, TypeElement classOrInterface,
			ClassDocumentation cd) {
		try {
			Path path = path(destinationDir, packageElement, classOrInterface);
			try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
				mapper.writerFor(ClassDocumentation.class).writeValue(writer, cd);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new DocletAbortException("Error writing file: " + e);
		}
	}

	private static Path path(String destinationDir, PackageElement packageElement,
			TypeElement classOrInterface) throws IOException {
		String packageName = packageElement.getQualifiedName().toString();
		String packageDir = packageName.replace(".", File.separator);
		Path packagePath = Paths.get(packageDir);

		final Path path;
		if (destinationDir != null) {
			path = Paths.get(destinationDir).resolve(packageDir);
		}
		else {
			path = packagePath;
		}

		Files.createDirectories(path);

		return path.resolve(classOrInterface.getSimpleName() + ".json");
	}

	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		return mapper;
	}
}
