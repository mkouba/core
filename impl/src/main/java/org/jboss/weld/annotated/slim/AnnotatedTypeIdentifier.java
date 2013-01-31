/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.annotated.slim;

import java.lang.reflect.Type;

import javax.enterprise.inject.New;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.weld.annotated.Identifier;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedType;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.util.AnnotatedTypes;
import org.jboss.weld.util.Types;

import com.google.common.base.Objects;

/**
 * An identifier for a an {@link AnnotatedType} The identifier is composed of four parts:
 *
 * <ul>
 *    <li>The identifier of the {@link BeanDeploymentArchive} which the type resides in. This allows different {@link BeanDeploymentArchive}s to bundle classes with the same name.</li>
 *    <li>The declaring class name.</li>
 *    <li>An optional suffix. The suffix is used for two purposes.</li>
 *    <ul>
 *       <li>If a {@link BackedAnnotatedType} is created for a parameterized type (a {@link New} injection point), suffix is set to an identifier of that type</li>
 *       <li>For an {@link UnbackedAnnotatedType} suffix holds the type identifier provided by the extension or calculated based on the type's qualities (see {@link AnnotatedTypes#createTypeId(AnnotatedType)})</li>
 *    </ul>
 *    <li>Modified flag which indicates whether this is an identifier for an {@link AnnotatedType} which has been modified during {@link ProcessAnnotatedType} event notification.</li>
 * </ul>
 *
 * @author Jozef Hartinger
 *
 */
public class AnnotatedTypeIdentifier implements Identifier {

    public static String NULL_BDA_ID = AnnotatedTypeIdentifier.class.getName() + ".null";

    public static String SYNTHETIC_ANNOTATION_SUFFIX = "syntheticAnnotation";

    public static AnnotatedTypeIdentifier forBackedAnnotatedType(Class<?> javaClass, Type type, String bdaId) {
        return new AnnotatedTypeIdentifier(bdaId, javaClass.getName(), getTypeId(type), false);
    }

    public static AnnotatedTypeIdentifier forModifiedAnnotatedType(AnnotatedTypeIdentifier originalIdentifier) {
        if (originalIdentifier.modified) {
            throw new IllegalArgumentException("Cannot create a modified identifier for an already modified identifier.");
        }
        return new AnnotatedTypeIdentifier(originalIdentifier.bdaId, originalIdentifier.className,
                originalIdentifier.suffix, true);
    }

    public static AnnotatedTypeIdentifier of(String bdaId, String className, String suffix, boolean modified) {
        return new AnnotatedTypeIdentifier(bdaId, className, suffix, modified);
    }

    private static final long serialVersionUID = -264184070652700144L;

    private final String bdaId;
    private final String className;
    private final String suffix;
    private final boolean modified;
    private final int hashCode;

    private AnnotatedTypeIdentifier(String bdaId, String className, String suffix, boolean modified) {
        this.bdaId = bdaId;
        this.className = className;
        this.suffix = suffix;
        this.modified = modified;
        this.hashCode = Objects.hashCode(bdaId, className, suffix, modified);
    }

    private static String getTypeId(Type type) {
        if (type == null || type instanceof Class<?>) {
            return null;
        }
        return Types.getTypeId(type);
    }

    public String getBdaId() {
        return bdaId;
    }

    public String getClassName() {
        return className;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AnnotatedTypeIdentifier) {
            AnnotatedTypeIdentifier they = (AnnotatedTypeIdentifier) obj;
            return Objects.equal(bdaId, they.bdaId) && Objects.equal(className, they.className)
                    && Objects.equal(suffix, they.suffix) && Objects.equal(modified, they.modified);
        }
        return false;
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(bdaId);
        builder.append(ID_SEPARATOR);
        builder.append(className);
        builder.append(ID_SEPARATOR);
        builder.append(suffix);
        builder.append(ID_SEPARATOR);
        builder.append(modified);
        return builder.toString();
    }

    @Override
    public String toString() {
        return "AnnotatedTypeIdentifier [bdaId=" + bdaId + ", className=" + className + ", suffix=" + suffix + ", modified=" + modified + "]";
    }
}
