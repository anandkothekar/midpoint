/*
 * Copyright (c) 2010-2014 Evolveum
 *
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
 */
package com.evolveum.midpoint.model;

import com.evolveum.midpoint.model.api.ModelPort;
import com.evolveum.midpoint.model.impl.ModelWebService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteChanges;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteChangesResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScripts;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.FindShadowOwner;
import com.evolveum.midpoint.xml.ns._public.model.model_3.FindShadowOwnerResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.GetObject;
import com.evolveum.midpoint.xml.ns._public.model.model_3.GetObjectResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ImportFromResource;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ImportFromResourceResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.NotifyChange;
import com.evolveum.midpoint.xml.ns._public.model.model_3.NotifyChangeResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.SearchObjects;
import com.evolveum.midpoint.xml.ns._public.model.model_3.SearchObjectsResponse;
import com.evolveum.midpoint.xml.ns._public.model.model_3.TestResource;
import com.evolveum.midpoint.xml.ns._public.model.model_3.TestResourceResponse;
import org.jcp.xml.dsig.internal.dom.DOMUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Holder;
import javax.xml.ws.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 
 * @author mederly
 * 
 */
@Service
public class ModelWebServiceRaw implements Provider<DOMSource> {

	private static final Trace LOGGER = TraceManager.getTrace(ModelWebServiceRaw.class);

    public static final String NS_SOAP11_ENV = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String NS_SOAP11_ENV_PREFIX = "SOAP-ENV";
    public static final QName SOAP11_FAULT = new QName(NS_SOAP11_ENV, "Fault");
    public static final QName SOAP11_FAULTCODE = new QName("", "faultcode");
    public static final String SOAP11_FAULTCODE_SERVER = NS_SOAP11_ENV_PREFIX + ":Server";
    public static final QName SOAP11_FAULTSTRING = new QName("", "faultstring");
    public static final QName SOAP11_FAULTACTOR = new QName("", "faultactor");
    public static final QName SOAP11_FAULT_DETAIL = new QName("", "detail");
    public static final String ACTOR = "TODO";

    @Autowired
	private ModelWebService ws;
	
	@Autowired
	private PrismContext prismContext;
	
    @Override
    public DOMSource invoke(DOMSource request) {
        try {
            return invokeAllowingFaults(request);
        } catch (FaultMessage faultMessage) {
            return serializeFaultMessage(faultMessage);
        }
    }

    public DOMSource invokeAllowingFaults(DOMSource request) throws FaultMessage {
        Node rootNode = request.getNode();
        Element rootElement;
        if (rootNode instanceof Document) {
            rootElement = ((Document) rootNode).getDocumentElement();
        } else if (rootNode instanceof Element) {
            rootElement = (Element) rootNode;
        } else {
            throw ws.createIllegalArgumentFault("Unexpected DOM node type: " + rootNode);
        }

        Object requestObject;
        try {
            requestObject = prismContext.parseAnyValue(rootElement);
        } catch (SchemaException e) {
            throw ws.createIllegalArgumentFault("Couldn't parse SOAP request body because of schema exception: " + e.getMessage());
        }

        Node response;
        Holder<OperationResultType> operationResultTypeHolder = new Holder<>();
        try {
            if (requestObject instanceof GetObject) {
                GetObject g = (GetObject) requestObject;
                Holder<ObjectType> objectTypeHolder = new Holder<>();
                ws.getObject(g.getObjectType(), g.getOid(), g.getOptions(), objectTypeHolder, operationResultTypeHolder);
                GetObjectResponse gr = new GetObjectResponse();
                gr.setObject(objectTypeHolder.value);
                gr.setResult(operationResultTypeHolder.value);
                response = prismContext.serializeAnyDataToElement(gr, ModelPort.GET_OBJECT_RESPONSE);
            } else if (requestObject instanceof SearchObjects) {
                SearchObjects s = (SearchObjects) requestObject;
                Holder<ObjectListType> objectListTypeHolder = new Holder<>();
                ws.searchObjects(s.getObjectType(), s.getQuery(), s.getOptions(), objectListTypeHolder, operationResultTypeHolder);
                SearchObjectsResponse sr = new SearchObjectsResponse();
                sr.setObjectList(objectListTypeHolder.value);
                sr.setResult(operationResultTypeHolder.value);
                response = prismContext.serializeAnyDataToElement(sr, ModelPort.SEARCH_OBJECTS_RESPONSE);
            } else if (requestObject instanceof ExecuteChanges) {
                ExecuteChanges e = (ExecuteChanges) requestObject;
                ObjectDeltaOperationListType objectDeltaOperationListType = ws.executeChanges(e.getDeltaList(), e.getOptions());
                ExecuteChangesResponse er = new ExecuteChangesResponse();
                er.setDeltaOperationList(objectDeltaOperationListType);
                response = prismContext.serializeAnyDataToElement(er, ModelPort.EXECUTE_CHANGES_RESPONSE);
            } else if (requestObject instanceof FindShadowOwner) {
                FindShadowOwner f = (FindShadowOwner) requestObject;
                Holder<UserType> userTypeHolder = new Holder<>();
                ws.findShadowOwner(f.getShadowOid(), userTypeHolder, operationResultTypeHolder);
                FindShadowOwnerResponse fsr = new FindShadowOwnerResponse();
                fsr.setUser(userTypeHolder.value);
                fsr.setResult(operationResultTypeHolder.value);
                response = prismContext.serializeAnyDataToElement(fsr, ModelPort.FIND_SHADOW_OWNER_RESPONSE);
            } else if (requestObject instanceof TestResource) {
                TestResource tr = (TestResource) requestObject;
                OperationResultType operationResultType = ws.testResource(tr.getResourceOid());
                TestResourceResponse trr = new TestResourceResponse();
                trr.setResult(operationResultType);
                response = prismContext.serializeAnyDataToElement(trr, ModelPort.TEST_RESOURCE_RESPONSE);
            } else if (requestObject instanceof ExecuteScripts) {
                ExecuteScripts es = (ExecuteScripts) requestObject;
                ExecuteScriptsResponse esr = ws.executeScripts(es);
                response = prismContext.serializeAnyDataToElement(esr, ModelPort.EXECUTE_SCRIPTS_RESPONSE);
            } else if (requestObject instanceof ImportFromResource) {
                ImportFromResource ifr = (ImportFromResource) requestObject;
                TaskType taskType = ws.importFromResource(ifr.getResourceOid(), ifr.getObjectClass());
                ImportFromResourceResponse ifrr = new ImportFromResourceResponse();
                ifrr.setTask(taskType);
                response = prismContext.serializeAnyDataToElement(ifrr, ModelPort.IMPORT_FROM_RESOURCE_RESPONSE);
            } else if (requestObject instanceof NotifyChange) {
                NotifyChange nc = (NotifyChange) requestObject;
                TaskType taskType = ws.notifyChange(nc.getChangeDescription());
                NotifyChangeResponse ncr = new NotifyChangeResponse();
                ncr.setTask(taskType);
                response = prismContext.serializeAnyDataToElement(ncr, ModelPort.NOTIFY_CHANGE_RESPONSE);
            } else {
                throw ws.createIllegalArgumentFault("Unsupported request type: " + requestObject);
            }
        } catch (SchemaException e) {
            throw createSystemFault(e, operationResultTypeHolder.value);
        }
        return new DOMSource(response);
    }

    private DOMSource serializeFaultMessage(FaultMessage faultMessage) {
        Element faultElement = DOMUtil.createElement(SOAP11_FAULT);
        Element faultCodeElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTCODE);
        faultCodeElement.setTextContent(SOAP11_FAULTCODE_SERVER);           // todo here is a constant until we have a mechanism to determine the correct value (client / server)
        Element faultStringElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTSTRING);
        faultStringElement.setTextContent(faultMessage.getMessage());
        Element faultActorElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULTACTOR);
        faultActorElement.setTextContent("TODO");               // todo
        Element faultDetailElement = DOMUtil.createSubElement(faultElement, SOAP11_FAULT_DETAIL);
        faultDetailElement.setTextContent(getStackTraceAsString(faultMessage));
        return new DOMSource(faultElement.getOwnerDocument());
    }

    private String getStackTraceAsString(FaultMessage faultMessage) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        faultMessage.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private FaultMessage createSystemFault(Exception ex, OperationResultType resultType) {
		if (resultType != null) {
            return ws.createSystemFault(ex, OperationResult.createOperationResult(resultType));
		} else {
            return ws.createSystemFault(ex, null);
        }
	}
}