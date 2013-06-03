/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditReportDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.ComponentFeedbackPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import java.util.Date;

/**
 * @author lazyman
 */
public class AuditPopupPanel extends SimplePanel<AuditReportDto> {

    private static final String ID_FORM = "form";
    private static final String ID_DATE_FROM = "dateFrom";
    private static final String ID_DATE_TO = "dateTo";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_RUN = "run";

    public AuditPopupPanel(String id, IModel<AuditReportDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        DateTimeField dateFrom = new DateTimeField(ID_DATE_FROM, new PropertyModel<Date>(getModel(),
                AuditReportDto.F_FROM)) {

            @Override
            protected boolean use12HourFormat() {
                return false;
            }
        };
        dateFrom.setRequired(true);
        form.add(dateFrom);

        DateTimeField dateTo = new DateTimeField(ID_DATE_TO, new PropertyModel<Date>(getModel(), AuditReportDto.F_TO)) {

            @Override
            protected boolean use12HourFormat() {
                return false;
            }
        };
        dateTo.setRequired(true);
        form.add(dateTo);

        form.add(new DateValidator(dateFrom, dateTo));

        AjaxSubmitLinkButton run = new AjaxSubmitLinkButton(ID_RUN, ButtonType.POSITIVE,
                createStringResource("PageBase.button.run")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onRunPerformed(target);
            }
        };
        form.add(run);
    }

    protected void onRunPerformed(AjaxRequestTarget target) {

    }

    private static class DateValidator extends AbstractFormValidator {

        private DateTimeField dateFrom;
        private DateTimeField dateTo;

        private DateValidator(DateTimeField dateFrom, DateTimeField dateTo) {
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        }

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[]{dateFrom, dateTo};
        }

        @Override
        public void validate(Form<?> form) {
            Date from = dateFrom.getConvertedInput();
            Date to = dateTo.getConvertedInput();

            if (from == null || to == null) {
                return;
            }

            if (from.after(to)) {
                form.error(form.getString("AuditPopupPanel.message.fromAfterTo"));
            }
        }
    }
}