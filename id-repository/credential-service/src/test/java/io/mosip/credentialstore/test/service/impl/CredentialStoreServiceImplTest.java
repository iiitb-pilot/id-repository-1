package io.mosip.credentialstore.test.service.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataSharePolicies;
import io.mosip.credentialstore.dto.Policies;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.dto.Type;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.service.impl.CredentialStoreServiceImpl;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.credentialstore.util.WebSubUtil;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BDBInfoType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class CredentialStoreServiceImplTest {

	
	@Mock
	private PolicyUtil policyUtil;

	@Mock
	private IdrepositaryUtil idrepositaryUtil;
	/** The id auth provider. */
	@Mock
	@Qualifier("idauth")
	CredentialProvider idAuthProvider;

	@Mock
	@Qualifier("default")
	CredentialProvider credentialDefaultProvider;
	
	@Mock
	@Qualifier("qrcode")
	CredentialProvider qrCodeProvider;
	
	/** The data share util. */
	@Mock
	private DataShareUtil dataShareUtil;

	/** The web sub util. */
	@Mock
	private WebSubUtil webSubUtil;
	
	@Mock
	Utilities utilities;

	@Mock
	private Environment env;
	
	@Mock
	private CbeffUtil cbeffutil;

	
	
	@Mock
	private AuditHelper auditHelper;
	
	@InjectMocks
	private CredentialStoreServiceImpl credentialStoreServiceImpl;
	
	
	/** The id response. */
	private IdResponseDTO idResponse = new IdResponseDTO();

	/** The response. */
	private ResponseDTO response = new ResponseDTO();
	
	@Before
	public void setUp() throws Exception {
		
		Mockito.when(env.getProperty("mosip.credential.service.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		
		
		Mockito.when(env.getProperty("credentialType.formatter.MOSIP"))
		.thenReturn("CredentialDefaultProvider");
		PolicyDetailResponseDto policyDetailResponseDto = new PolicyDetailResponseDto();
		policyDetailResponseDto.setPolicyId("45678451034176");
		policyDetailResponseDto.setVersion("1.1");
		policyDetailResponseDto.setPolicyName("Digital QR Code Policy");
		policyDetailResponseDto.setPolicyDesc("");
		DataSharePolicies dataSharePolicies = new DataSharePolicies();
		dataSharePolicies.setEncryptionType("partnerBased");
		dataSharePolicies.setShareDomain("mosip.io");
		dataSharePolicies.setTransactionsAllowed(2);
		dataSharePolicies.setValidForInMinutes(30);
		Policies policies = new Policies();
		policies.setDataSharePolicies(dataSharePolicies);
		List<ShareableAttribute> sharableAttributesList = new ArrayList<ShareableAttribute>();
		ShareableAttribute shareableAttribute1 = new ShareableAttribute();
		shareableAttribute1.setAttributeName("fullName");
		shareableAttribute1.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute1);
		ShareableAttribute shareableAttribute2 = new ShareableAttribute();
		shareableAttribute2.setAttributeName("dateOfBirth");
		shareableAttribute2.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute2);
		ShareableAttribute shareableAttribute3 = new ShareableAttribute();
		shareableAttribute3.setAttributeName("face");
		shareableAttribute3.setEncrypted(true);
		shareableAttribute3.setFormat("extraction");
		sharableAttributesList.add(shareableAttribute3);
		policies.setShareableAttributes(sharableAttributesList);
		policyDetailResponseDto.setPolicies(policies);
		Map<String, String> map1 = new HashMap<>();
		map1.put("UIN", "4238135072");
		JSONObject jsonObject = new JSONObject(map1);

		LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("language", "eng");
		map.put("value", "raghav");
		JSONObject j1 = new JSONObject(map);

		Map<String, String> map2 = new HashMap<>();
		map2.put("language", "ara");
		map2.put("value", "Alok");
		JSONObject j2 = new JSONObject(map2);
		JSONArray array = new JSONArray();
		array.add(j1);
		array.add(j2);
		identityMap.put("fullName", array);
		identityMap.put("gender", array);
		identityMap.put("addressLine1", array);
		identityMap.put("addressLine2", array);
		identityMap.put("addressLine3", array);
		identityMap.put("city", array);
		identityMap.put("province", array);
		identityMap.put("region", array);
		identityMap.put("dateOfBirth", "1980/11/14");
		identityMap.put("phone", "9967878787");
		identityMap.put("email", "raghavdce@gmail.com");
		identityMap.put("postalCode", "900900");
		identityMap.put("proofOfAddress", j2);

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		doc1.setValue("individual biometric value");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		byte[] bioBytes = "individual biometric value".getBytes();
		List<SingleType> singleList = new ArrayList<>();
		singleList.add(SingleType.FACE);
		BIRType type = new BIRType();
		type.setBDB(bioBytes);
		BDBInfoType bdbinfotype = new BDBInfoType();
		bdbinfotype.setType(singleList);
		type.setBDBInfo(bdbinfotype);
		List<BIRType> birtypeList = new ArrayList<>();
		birtypeList.add(type);
		response.setDocuments(docList);
		idResponse.setResponse(response);
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birtypeList);

		Mockito.when(utilities.generateId()).thenReturn("123456");
	 	Mockito.when(policyUtil.getPolicyDetail(Mockito.anyString(), Mockito.anyString())).thenReturn(policyDetailResponseDto);
		Mockito.when(idrepositaryUtil.getData(Mockito.any(),Mockito.any()))
		.thenReturn(idResponse);
		DataProviderResponse dataProviderResponse=new DataProviderResponse();
		Mockito.when(credentialDefaultProvider.getFormattedCredentialData(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataProviderResponse);
		DataShare dataShare=new DataShare();
		Mockito.when(dataShareUtil.getDataShare(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dataShare);
		
	}
	
	@Test
	public void testCreateCredentialIssueSuccess() {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getResponse().getStatus(),"DONE");
	}
	@Test
	public void testCreateCredentialIssuePolicyFailure() {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
	 	Mockito.when(policyUtil.getPolicyDetail(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testApiNotAccessibleException() throws ApiNotAccessibleException, IdRepoException, IOException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		ApiNotAccessibleException e = new ApiNotAccessibleException();
		Mockito.when(idrepositaryUtil.getData(Mockito.any(),Mockito.any()))
		.thenThrow(e);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testIdRepoException() throws ApiNotAccessibleException, IdRepoException, IOException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		IdRepoException e = new IdRepoException();
		Mockito.when(idrepositaryUtil.getData(Mockito.any(),Mockito.any()))
		.thenThrow(e);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testCredentialFormatterException() throws  CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		CredentialFormatterException e = new CredentialFormatterException();
		Mockito.when(credentialDefaultProvider.getFormattedCredentialData(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testIOException() throws ApiNotAccessibleException, IdRepoException, IOException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		IOException e = new IOException();
		Mockito.when(idrepositaryUtil.getData(Mockito.any(),Mockito.any()))
		.thenThrow(e);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testWebSubClientException() throws ApiNotAccessibleException, IdRepoException, IOException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		WebSubClientException e = new WebSubClientException("","");
		Mockito.doThrow(e).when(webSubUtil).publishSuccess(Mockito.any(),Mockito.any());

		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorMessage());
	}
	
	@Test
	public void testException(){
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Mockito.when(env.getProperty("credentialType.formatter.MOSIP"))
		.thenReturn(null);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
	}
	@Test
	public void testgetCredentialTypes() {
		List<Type> typeList=new ArrayList<>();
		Type type=new Type();
		typeList.add(type);
		Mockito.when(utilities.getTypes(Mockito.any(), Mockito.any())).thenReturn(typeList);
		CredentialTypeResponse credentialTypeResponse=credentialStoreServiceImpl.getCredentialTypes();
		 assertEquals(credentialTypeResponse.getCredentialTypes(),typeList);
	}
	@Test
	public void testCreateCredentialIssueSuccessWithSharedAttribute() {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		List<String> sharableAttributes=new ArrayList<>();
		sharableAttributes.add("face");
		credentialServiceRequestDto.setSharableAttributes(sharableAttributes);
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getResponse().getStatus(),"DONE");
	}

	@Test
	public void testDataShareException() throws ApiNotAccessibleException, IdRepoException, IOException, DataShareException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType("mosip");
		credentialServiceRequestDto.setId("4238135072");
		Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		DataShareException e = new DataShareException();
		Mockito.when(dataShareUtil.getDataShare(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(e);

		CredentialServiceResponseDto credentialServiceResponseDto=credentialStoreServiceImpl.createCredentialIssuance(credentialServiceRequestDto);
	    assertEquals(credentialServiceResponseDto.getErrors().get(0).getMessage(),CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
	}
}