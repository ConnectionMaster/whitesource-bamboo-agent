/*
 * Copyright (C) 2012 WhiteSource Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.whitesource.bamboo.plugins;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class AgentTaskConfigurator extends AbstractTaskConfigurator
{
    public static final String ORGANIZATION_TOKEN = "organizationToken";
    public static final String PROJECT_TOKEN = "projectToken";
    public static final String INCLUDES_PATTERN = "includesPattern";
    public static final String EXCLUDES_PATTERN = "excludesPattern";
    private static final Set<String> FIELD_COLLECTION = ImmutableSet.<String> builder()
            .add(ORGANIZATION_TOKEN, PROJECT_TOKEN, INCLUDES_PATTERN, EXCLUDES_PATTERN).build();
    private static final String DEFAULT_INCLUDES_PATTERN = "lib/*.jar";
    private TextProvider textProvider;  // KLUDGE: unused currently, see validate().

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        context.put(INCLUDES_PATTERN, DEFAULT_INCLUDES_PATTERN);
        context.put("mode", "create");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
        context.put("mode", "edit");
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context,
            @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition,
                Iterables.concat(FIELD_COLLECTION, getFieldCollection()));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        // KLUDGE/REVIEW: the i18n text should be provided by
        // textProvider.getText() apparently as per the code generated
        // via 'atlas-create-bamboo-plugin', however, this does not work at all,
        // see https://answers.atlassian.com/questions/20566 for a discussion;
        // replacing the call with getI18nBean().getText() works fine though.
        final String organizationTokenValue = params.getString("organizationToken");
        if (StringUtils.isEmpty(organizationTokenValue))
        {
            errorCollection.addError("organizationToken",
                    getI18nBean().getText("org.whitesource.bamboo.plugins.organizationToken.error"));
        }
        final String projectTokenValue = params.getString("projectToken");
        if (StringUtils.isEmpty(projectTokenValue))
        {
            errorCollection.addError("projectToken",
                    getI18nBean().getText("org.whitesource.bamboo.plugins.projectToken.error"));
        }
        final String includesPatternValue = params.getString("includesPattern");
        if (StringUtils.isEmpty(includesPatternValue))
        {
            errorCollection.addError("includesPattern",
                    getI18nBean().getText("org.whitesource.bamboo.plugins.includesPattern.error"));
        }
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }

    protected Set<String> getFieldCollection()
    {
        return FIELD_COLLECTION;
    }
}
