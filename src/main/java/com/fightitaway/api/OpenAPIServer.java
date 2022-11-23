package com.fightitaway.api;

import java.security.SecureRandom;

import org.json.JSONObject;

import com.fightitaway.common.HttpClientHandler;
import com.fightitaway.service.ConfirmPaymentRequest;
import com.fightitaway.service.CreateOrder;
import com.fightitaway.service.CreateProfileRequest;
import com.fightitaway.service.FightitawayService;
import com.fightitaway.service.PaymentGatewayService;
import com.fightitaway.service.impl.FightitawayServiceImpl;
import com.fightitaway.service.impl.PaymentGatewayServiceImpl;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * Application triggering point.
 * 
 * @author Smartdietplanner
 * @since 08-Aug-2020
 *
 */
public class OpenAPIServer extends BaseRxVerticle {
		
	private static final Logger logger = LoggerFactory.getLogger(OpenAPIServer.class);
	
	public static String INSTANCE_NAME;
	
	public int  c=1;
	JWTAuth authProvider;
	private FightitawayService fightitwayService;
	private PaymentGatewayService paymentService;
	
	private PingHandler pingHandler;
    private FailureHandler failureHandler;
	
	static final Integer LENGTH = 15;
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static SecureRandom rnd = new SecureRandom();

	public String randomString() {
		StringBuilder sb = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));

		return "[" + sb.toString() + "]";
	}
    
	@SuppressWarnings("deprecation")
	@Override
	public void start() throws Exception {

		OpenAPI3RouterFactory.create(vertx.getDelegate(), "webroot/openapi_inapp.yaml", ar -> {
			
			if (ar.succeeded()) {
				
				this.pingHandler = new PingHandler();
		        this.failureHandler = new FailureHandler();
				
				JsonObject confJson = config();

				logger.info("##### CONFIG -->> " + confJson.toString());
				
				Integer port = confJson.getJsonObject("server").getInteger("port");
				
				String host = confJson.getJsonObject("server").getString("host");

				boolean isSecure = confJson.getJsonObject("server").getBoolean("isSecure");
				
				String certPath = confJson.getJsonObject("server").getString("certPath");
				
				String certPassWord = confJson.getJsonObject("server").getString("certPassWord");
				
				Long timeout = confJson.getLong("timeout",10000l);
				
				WebClientOptions options = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
				
				io.vertx.rxjava.ext.web.client.WebClient client = io.vertx.rxjava.ext.web.client.WebClient.create(vertx, options);
				
				HttpClientHandler clientHandler=new HttpClientHandler(client, confJson);
			
				
				ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
				fightitwayService=new FightitawayServiceImpl(vertx,config());
				paymentService=new PaymentGatewayServiceImpl(vertx,config(),clientHandler);				
				APIHandler apiHandler=new APIHandler(fightitwayService);
				
				binder.setAddress("fightitaway-event-bus").register(FightitawayService.class, fightitwayService);
				logger.info("##### APPLICATION STARTED @ [" + host + ":" + port + "]");
				
						authProvider = JWTAuth.create(vertx.getDelegate(), new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions()
						.setAlgorithm("ES256")
						.setSecretKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/56PGdmRtQCykZdYWBOUfsRlXdwH53GZJMaljMYke3ShRANCAAQ9vS5GXjQZcLQ+B8EHgC2hJO+Zmaucv1E7D/p1MVwiX2qAZVyCx1ub/PWhjlArpDn0FIwRnQRbUriaL9+KASNV")
						.setPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPb0uRl40GXC0PgfBB4AtoSTvmZmrnL9ROw/6dTFcIl9qgGVcgsdbm/z1oY5QK6Q59BSMEZ0EW1K4mi/figEjVQ==")));
			 
				OpenAPI3RouterFactory factory = ar.result();		
				factory.addSecurityHandler("ApiKey", JWTAuthHandler.create(authProvider));
				
				factory.addHandlerByOperationId("getDefaultDetailOld", rc -> {
					String traceId = randomString();
					fightitwayService.getDefaultDeatil(traceId, resultHandler ->{
						rc.response().end(resultHandler.result().encodePrettily());
					});
				});

				factory.addHandlerByOperationId("dashboard", apiHandler::getDashboard);
				factory.addHandlerByOperationId("getDefaultDetail", apiHandler::getDefaultDetail);
				factory.addHandlerByOperationId("profile", apiHandler::profile);
				factory.addHandlerByOperationId("updateDemographic", apiHandler::updateDemographic);
				factory.addHandlerByOperationId("updateLifeStyle", apiHandler::updateLifeStyle);
				factory.addHandlerByOperationId("updateDiet", apiHandler::updateDiet);
				factory.addHandlerByOperationId("dietPlansV2", apiHandler::dietPlansV3Cache);
				factory.addHandlerByOperationId("createCustomerDiet", apiHandler::createCustomerDiet);
				factory.addHandlerByOperationId("getCustomerDiets", apiHandler::getCustomerDiets);
				factory.addHandlerByOperationId("createHabit", apiHandler::createHabit);
				factory.addHandlerByOperationId("getHabit", apiHandler::getHabit);
				factory.addHandlerByOperationId("createCustomerHabit", apiHandler::createCustomerHabit);
				factory.addHandlerByOperationId("getCustomerHabit", apiHandler::getCustomerHabit);
				factory.addHandlerByOperationId("updateCustomerHabit", apiHandler::updateCustomerHabit);
				factory.addHandlerByOperationId("getCouponList", apiHandler::getCouponListFromMaster);
				factory.addHandlerByOperationId("habitsForUpdate", apiHandler::habitsForUpdate);
				factory.addHandlerByOperationId("deleteCustomerHabit", apiHandler::deleteCustomerHabit);
				factory.addHandlerByOperationId("updateCustomerWeight", apiHandler::updateCustomerWeight);
				factory.addHandlerByOperationId("getCustomerCurrentWeight", apiHandler::getCustomerCurrentWeight);
				factory.addHandlerByOperationId("getCustomerWeightGrphData", apiHandler::getCustomerWeightGrphData);
				factory.addHandlerByOperationId("dietPlanForOption", apiHandler::dietPlanForOption);
				factory.addHandlerByOperationId("addCustDietPref", apiHandler::addCustDietPref);
				factory.addHandlerByOperationId("getCustDietPref", apiHandler::getCustDietPref);
				factory.addHandlerByOperationId("returnDietPlansV3Cache", apiHandler::returnDietPlansV3Cache);
				factory.addHandlerByOperationId("getSpecificDetailsByCommunity", apiHandler::getSpecificDetailsByCommunity);
				factory.addHandlerByOperationId("dietPlanRefreshOption", apiHandler::dietPlanRefreshOption);
				// NORMAL DIETPLAN
				if (!confJson.getBoolean("isCacheRequired"))  // WITHOUT CAHCE
					factory.addHandlerByOperationId("dietPlansV3", apiHandler::dietPlansV3);
				else // WITH CACHE
					factory.addHandlerByOperationId("dietPlansV3", apiHandler::dietPlansV3Cache);
				factory.addHandlerByOperationId("addCustDietPrefV2", apiHandler::addCustDietPrefV2);
				factory.addHandlerByOperationId("getCustDietDetailsOnEdit", apiHandler::getCustDietDetailsOnEdit);
				factory.addHandlerByOperationId("getDietPlanTimings", apiHandler::getDietPlanTimings);
				factory.addHandlerByOperationId("sendEmail", apiHandler::sendEmail);
				factory.addHandlerByOperationId("cacheddata", apiHandler::getDietPlanCachedata);
				factory.addHandlerByOperationId("profileRemove", apiHandler::profileRemove);
				factory.addHandlerByOperationId("addTnC", apiHandler::addTnC);
				factory.addHandlerByOperationId("getOnePlan", apiHandler::getOnePlan);
				factory.addHandlerByOperationId("subscribePlanByCoupon", apiHandler::subscribePlanByCoupon);
				factory.addHandlerByOperationId("fetchFood", apiHandler::fetchFood);
				factory.addHandlerByOperationId("saveCustCaloriesBurnt", apiHandler::saveCustCaloriesBurnt);
				factory.addHandlerByOperationId("waterTips", apiHandler::waterTips);
				factory.addHandlerByOperationId("saveWaterDrank", apiHandler::saveWaterDrank);
				factory.addHandlerByOperationId("waterRecommendation", apiHandler::waterRecommendation);
				factory.addHandlerByOperationId("saveWaterReminder", apiHandler::saveWaterReminder);
				factory.addHandlerByOperationId("fetchWaterReminder", apiHandler::fetchWaterReminder);
				factory.addHandlerByOperationId("fetchCaloriesHistory", apiHandler::fetchCaloriesHistory);
				factory.addHandlerByOperationId("fetchTargetCalories", apiHandler::fetchTargetCalories);
				factory.addHandlerByOperationId("getCustAnalytics", apiHandler::getCustAnalytics);
				factory.addHandlerByOperationId("refreshFoods", apiHandler::refreshFoods);
				factory.addHandlerByOperationId("getCustPaymentAnalytics", apiHandler::getCustPaymentAnalytics);
				factory.addHandlerByOperationId("getDietPlanCacheAnalytics", apiHandler::getDietPlanCacheAnalytics);
				factory.addHandlerByOperationId("downloadDietPlanCacheAnalytics", apiHandler::downloadDietPlanCacheAnalytics);
				factory.addHandlerByOperationId("createEmptyProfile", apiHandler::createEmptyProfile);
				factory.addHandlerByOperationId("dietPlanSaveTime", apiHandler::dietPlanSaveTime);
				factory.addHandlerByOperationId("saveOrUpdateCustomerDietPlanTimings", apiHandler::saveOrUpdateCustomerDietPlanTimings);
				factory.addHandlerByOperationId("fetchPendingCust", apiHandler::fetchPendingCust);
				factory.addHandlerByOperationId("fetchnSaveCust", apiHandler::fetchnSaveCust);
				factory.addHandlerByOperationId("saveDetoxDietplanStatus", apiHandler::saveDetoxDietplanStatus);
				factory.addHandlerByOperationId("fetchHelp", apiHandler::fetchHelp);
				factory.addHandlerByOperationId("getCustPaymentDetails", apiHandler::getCustPaymentDetails);
				factory.addHandlerByOperationId("fetchDietPlans", apiHandler::fetchDietPlans);
				factory.addHandlerByOperationId("fetchAllCustomersDetails", apiHandler::fetchAllCustomersDetails);
				factory.addHandlerByOperationId("saveDetoxDefaultDays", apiHandler::saveDetoxDefaultDays);
				factory.addHandlerByOperationId("updatePaymentDetails", apiHandler::updatePaymentDetails);
				factory.addHandlerByOperationId("updateTargetWeight", apiHandler::updateTargetWeight);
				factory.addHandlerByOperationId("updateCurrentWeight", apiHandler::updateCurrentWeight);
				factory.addHandlerByOperationId("saveOrUpdateCustSurvey", apiHandler::saveOrUpdateCustSurvey);
				factory.addHandlerByOperationId("updateCustMobile", apiHandler::updateCustMobile);
				factory.addHandlerByOperationId("fetchDietPlanTimings", apiHandler::fetchDietPlanTimings);
				factory.addHandlerByOperationId("intlDietPlans", apiHandler::fetchDietPlanTimings);
				factory.addGlobalHandler(TimeoutHandler.create(timeout));
				
				// REFUND PAYMENT
				factory.addHandlerByOperationId("refundPayment", ctx -> {
					String traceId = randomString();
					String email = ctx.user().principal().getString("email");
					logger.info("##### OPENAPISERVER " + traceId + "-[" + email + "]     REFUND URI [" + email
							+ "]   -->> " + ctx.request().uri());
					logger.debug("##### OPENAPISERVER " + traceId + "-[" + email + "]                REFUND EMAIL -->> "
							+ email);
					JsonObject response = new JsonObject();
					paymentService.refund(email, ctx.request().uri(), traceId, rs -> {
						if (rs.succeeded()) {
							ctx.response().end(rs.result().encodePrettily());
						} else {
							response.put("code", "0001");
							response.put("message", rs.cause().getMessage());
							ctx.response().end(response.encodePrettily());
						}
					});
				});
				
				// CREATE ORDER
				factory.addHandlerByOperationId("createOrder", ctx -> {
					String traceId = randomString();
					String email = ctx.user().principal().getString("email");
					logger.info("##### OPENAPISERVER " + traceId + "    CREATEORDER [" + email + "]");
					logger.info("##### OPENAPISERVER " + traceId + "    CREATEORDER URI [" + email + "] -->> "
							+ ctx.request().uri());
					CreateOrder createOrder = new CreateOrder();
					createOrder.setAmount(ctx.getBodyAsJson().getDouble("amount"));
					createOrder.setCouponCode(ctx.getBodyAsJson().getString("couponCode", "CU5000"));
					createOrder.setEmail(email);
					logger.debug(
							"##### OPENAPISERVER " + traceId + " CREATEORDER OBJECT -->> " + createOrder.toString());
					paymentService.createOrder(createOrder, ctx.request().uri(), traceId, ha -> {
						JsonObject response = new JsonObject();
						if (ha.succeeded()) {
							response.put("code", "0000");
							response.put("message", "success");
							response = new JsonObject(Json.encode(ha.result()));
						} else {
							response.put("code", "0001");
							response.put("message", ha.cause().getMessage());
						}
						ctx.response().end(response.toString());
					});
				});
				
				// CONFIRM PAYMENT
				factory.addHandlerByOperationId("confirmPayment", ctx -> {
					String traceId = randomString();
					try {
						String email = ctx.user().principal().getString("email");
						logger.info("##### OPENAPISERVER " + traceId + "-[" + email + "] CONFIRMPAYMENT URI -->> "
								+ ctx.request().uri());
						logger.info("##### OPENAPISERVER " + traceId + "    CONFIRMPAYMENT [" + email + "]");
						JsonObject request = ctx.getBodyAsJson();
//						logger.info("##### OPENAPISERVER " + traceId + "-[" + email
//								+ "] CONFIRMPAYMENT FORM PARAMS -->> " + request.toString());
						boolean isEqual = true;
						String paymentId = request.getString("razorpay_payment_id");
						String razorpaySignature = request.getString("razorpay_signature");
						String orderId = request.getString("razorpay_order_id");
						String txnId = request.getString("txnId");
						JSONObject option = new JSONObject();
						option.put("razorpay_payment_id", paymentId);
						option.put("razorpay_order_id", orderId);
						option.put("razorpay_signature", razorpaySignature);
//						logger.info("##### OPENAPISERVER " + traceId + "-[" + email
//								+ "] (CONFIRMPAYMENT)               EMAIL -->> " + email);
						logger.info("##### OPENAPISERVER " + traceId + "-[" + email
								+ "] (CONFIRMPAYMENT) RAZORPAY PAYMENT ID -->> " + paymentId);
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
								+ "] (CONFIRMPAYMENT) RAZORPAY ORDER ID   -->> " + orderId);
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
								+ "] (CONFIRMPAYMENT) RAZORPAY SIGNATURE  -->> " + razorpaySignature);
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
								+ "] (CONFIRMPAYMENT) TRANSACTION ID      -->> " + txnId);
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
								+ "] (CONFIRMPAYMENT) API KEY             -->> "
								+ confJson.getJsonObject("pay").getString("apiKey"));
						
						if (isEqual) {
							ConfirmPaymentRequest paymentRequest = new ConfirmPaymentRequest();
							paymentRequest.setOrderId(orderId);
							paymentRequest.setPaymentId(paymentId);
							paymentRequest.setOrderId(orderId);
							paymentRequest.setStatus("SUCCESS");
							paymentRequest.setTxnId(txnId);
							paymentRequest.setEmailId(email);
							paymentRequest.setSignature(razorpaySignature);
							paymentRequest.setIsAccountFree(Boolean.FALSE);
							if (request.containsKey("free")) {
								logger.info("##### OPENAPISERVER " + traceId + "-[" + email
										+ "] (CONFIRMPAYMENT) FREE ACCOUNT");
								paymentRequest.setIsAccountFree(request.getBoolean("free"));
								paymentRequest.setCouponCode("CU0000");
							}
							paymentService.confirmPayment(paymentRequest, ctx.request().uri(), traceId, ha -> {
								if (ha.succeeded()) {
									logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
											+ "] (CONFIRMPAYMENT) HS -->> " + ha.result().toString());
									ctx.response().end(ha.result().toString());
								} else {
									logger.debug("##### OPENAPISERVER " + traceId + "-[" + email
											+ "] (CONFIRMPAYMENT) HA CAUSE -->> " + ha.cause().toString());
									ctx.response().end(new JsonObject().put("code", "1001").encodePrettily());
								}
							});
						} else {
							ctx.response().setStatusCode(400);
							ctx.response().end(new JsonObject().put("code", "1001").put("message", "signature mismatch")
									.encodePrettily());

						}
					} catch (Exception e) {
						e.printStackTrace();
						ctx.response().setStatusCode(400);
					}
				});
				
				// AUTHENTICATE - (CREATE/UPDATE PROFILE)
				factory.addHandlerByOperationId("authenticate", h -> {
					String traceId = randomString();
					logger.info("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
							+ "] (AUTHENTICATE) URI -->> " + h.request().uri());
					logger.info("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
							+ "] (AUTHENTICATE) ACCESS TOKEN COMPLETE REQUEST -->> " + h.request());
					JsonObject response = new JsonObject();
					String g_token = h.request().getParam("access_token");
					String source = h.request().getParam("provider");
					logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
							+ "] (AUTHENTICATE) ACCESS TOKEN COMPLETE REQUEST -->> " + h.request());
					logger.info("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
							+ "]            AUTHENTICATE SOURCE -->> " + source);
					String region = h.request().getParam("region");
					String appsource = h.request().getParam("appsource");
					String device = h.request().getParam("device");
					String os = h.request().getParam("os");

					if (null != source && ("iOS".equalsIgnoreCase(source) || "apple".equalsIgnoreCase(source))) {
						CreateProfileRequest profileRequest = new CreateProfileRequest();
						String email = h.request().getParam("email");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-APPLE) EMAIL -->> " + email);
						String name = h.request().getParam("name");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-APPLE) NAME -->> " + name);
						String givenName = h.request().getParam("firstName");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-APPLE) GIVEN NAME -->> " + givenName);
						String familyName = h.request().getParam("lastName");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-APPLE) FAMILY NAME -->> " + familyName);
						String country = (null == h.request().getParam("country")
								|| "".equalsIgnoreCase(h.request().getParam("country"))) ? "INDIA"
										: h.request().getParam("country").toUpperCase();
						profileRequest.setName(name);
						profileRequest.setFamily_name(familyName);
						profileRequest.setGiven_name(givenName);
						profileRequest.setCountry(country);
						profileRequest.setEmail(email);
						profileRequest.setLoginType(source);
						profileRequest.setRegion(null);
						if (null != region && !"".equalsIgnoreCase(region) && !"unknown".equalsIgnoreCase(region))
							profileRequest.setRegion(region);
						profileRequest.setAppsource(null);
						if (null != appsource && !"".equalsIgnoreCase(appsource) && !"unknown".equalsIgnoreCase(appsource))
							profileRequest.setAppsource(appsource);
						profileRequest.setDevice(null);
						if (null != device && !"".equalsIgnoreCase(device) && !"unknown".equalsIgnoreCase(device))
							profileRequest.setDevice(device);
						profileRequest.setOs(null);
						if (null != os && !"".equalsIgnoreCase(os) && !"unknown".equalsIgnoreCase(os))
							profileRequest.setOs(os);

						String token = authProvider.generateToken(
								new JsonObject().put("email", email).put("deviceId", "21321321"),
								new io.vertx.ext.jwt.JWTOptions().setAlgorithm("ES256"));

						response.put("access_token", token);
						response.put("email", profileRequest.getEmail());
						response.put("code", "0000");

						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-APPLE) RESPONSE -->> " + response);
						fightitwayService.createProfile(profileRequest, h.request().uri(), traceId, resultHandler -> {
							if (resultHandler.succeeded()) {
								logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
										+ "] (AUTHENTICATE-APPLE) PROFILE CREATED ");
								h.response().end(response.encodePrettily());
							} else {
								h.response().setStatusCode(401);
							}
						});
					} else if (null != source && "FACEBOOK".equalsIgnoreCase(source)) {
						CreateProfileRequest profileRequest = new CreateProfileRequest();
						String email = h.request().getParam("email");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-FACEBOOK) EMAIL -->> " + email);
						String name = h.request().getParam("name");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-FACEBOOK) NAME -->> " + name);
						String givenName = h.request().getParam("firstName");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-FACEBOOK) GIVEN NAME -->> " + givenName);
						String familyName = h.request().getParam("lastName");
						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-FACEBOOK) FAMILY NAME -->> " + familyName);
						profileRequest.setName(name);
						profileRequest.setFamily_name(familyName);
						profileRequest.setGiven_name(givenName);
						profileRequest.setEmail(email);
						profileRequest.setLoginType(source);
						profileRequest.setRegion(null);
						if (null != region && !"".equalsIgnoreCase(region) && !"unknown".equalsIgnoreCase(region))
							profileRequest.setRegion(region);
						profileRequest.setAppsource(null);
						if (null != appsource && !"".equalsIgnoreCase(appsource) && !"unknown".equalsIgnoreCase(appsource))
							profileRequest.setAppsource(appsource);
						profileRequest.setDevice(null);
						if (null != device && !"".equalsIgnoreCase(device) && !"unknown".equalsIgnoreCase(device))
							profileRequest.setDevice(device);
						profileRequest.setOs(null);
						if (null != os && !"".equalsIgnoreCase(os) && !"unknown".equalsIgnoreCase(os))
							profileRequest.setOs(os);

						String token = authProvider.generateToken(
								new JsonObject().put("access_token", g_token).put("email", profileRequest.getEmail()),
								new io.vertx.ext.jwt.JWTOptions().setAlgorithm("ES256"));
						response.put("access_token", token);
						response.put("email", profileRequest.getEmail());
						response.put("code", "0000");

						logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
								+ "] (AUTHENTICATE-FACEBOOK) RESPONSE -->> " + response);
						fightitwayService.createProfile(profileRequest, h.request().uri(), traceId, resultHandler -> {
							if (resultHandler.succeeded()) {
								logger.debug("##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
										+ "] (AUTHENTICATE-FACEBOOK) PROFILE [" + email + "] CREATED ");
								h.response().end(response.encodePrettily());
							} else {
								h.response().setStatusCode(401);
							}
						});
					} else {
						WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true)
								.setVerifyHost(false);
						optionsClient.setKeepAlive(false);
						WebClient webClient = WebClient.create(vertx.getDelegate(), optionsClient);
						webClient.getAbs("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + g_token + "")
								.send(ha -> {

									if (ha.succeeded() && ha.result().statusCode() == 200) {

										logger.debug(
												"##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
														+ "] (AUTHENTICATE) HA RESULT BODY -->> " + ha.result().body());
										CreateProfileRequest profileRequest = Json.decodeValue(ha.result().body(),
												CreateProfileRequest.class);
										String email = "";
										if (null == profileRequest.getEmail() || "".equals(profileRequest.getEmail())) {
											logger.debug("##### OPENAPISERVER " + traceId + "-["
													+ h.request().getParam("email")
													+ "] (AUTHENTICATE) EMAIL DOESN'T RECEIVED FROM GOOGLE API.");
											email = h.request().getParam("email");
											profileRequest.setEmail(email);
											logger.info("##### OPENAPISERVER " + traceId + "-[" + email
													+ "] (AUTHENTICATE) GMAIL AUTHENTICATE EMAIL -->> " + email);
										}

										String token = authProvider.generateToken(
												new JsonObject().put("access_token", g_token).put("email",
														profileRequest.getEmail()),
												new io.vertx.ext.jwt.JWTOptions().setAlgorithm("ES256"));
										profileRequest.setLoginType("GMAIL");
										profileRequest.setRegion(null);
										if (null != region && !"".equalsIgnoreCase(region) && !"unknown".equalsIgnoreCase(region))
											profileRequest.setRegion(region);
										profileRequest.setAppsource(null);
										if (null != appsource && !"".equalsIgnoreCase(appsource) && !"unknown".equalsIgnoreCase(appsource))
											profileRequest.setAppsource(appsource);
										profileRequest.setDevice(null);
										if (null != device && !"".equalsIgnoreCase(device) && !"unknown".equalsIgnoreCase(device))
											profileRequest.setDevice(device);
										profileRequest.setOs(null);
										if (null != os && !"".equalsIgnoreCase(os) && !"unknown".equalsIgnoreCase(os))
											profileRequest.setOs(os);
										response.put("access_token", token);
										response.put("email", profileRequest.getEmail());
										response.put("code", "0000");
										logger.debug(
												"##### OPENAPISERVER " + traceId + "-[" + h.request().getParam("email")
														+ "] (AUTHENTICATE) OPENAPISERVER RESPONSE -->> " + response);

										fightitwayService.createProfile(profileRequest, h.request().uri(), traceId,
												resultHandler -> {
													if (resultHandler.succeeded()) {
														logger.debug("##### OPENAPISERVER " + traceId + "-["
																+ h.request().getParam("email")
																+ "] (AUTHENTICATE) GMAIL PROFILE CREATED ");
														h.response().end(response.encodePrettily());
													} else {
														h.response().setStatusCode(401);
													}
												});
									} else {
										response.put("message", "invalid token");
										response.put("code", "401");
										h.response().end(response.encodePrettily());
									}

								});
					}
				});
				factory.mountServicesFromExtensions();
				
				factory.addGlobalHandler(h -> {
					String traceId = randomString();
					logger.debug("##### OPENAPISERVER " + traceId + " ADDGLOBALHEADER URI -->> " + h.request().uri());
					
					h.response().putHeader("content-type", "application/json")
							.putHeader("Access-Control-Allow-Origin", "*").putHeader("Access-Control-Max-Age", "3600")
							.putHeader("Access-Control-Allow-Credentials", "true")
							.putHeader("Access-Control-Allow-Headers","Authorization,User-Agent,Connection,Host,Accept-Language,Accept-Encoding,Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method,OPTIONS,Referer,Accept")
							.putHeader("Access-Control-Allow-Methods", "GET, POST,PUT, DELETE, OPTIONS");
					h.next();
				});

				
				Router router = factory.getRouter();
				
				
				router.get("/ping").handler(pingHandler).failureHandler(failureHandler);

				
				
				router.get("/getToken").handler(this::getToken);
				
				router.get("/v3").handler( ha ->{
					String traceId = randomString();
					fightitwayService.getTodayDietList(ha.request().getParam("email"), ha.request().getParam("date"), ha.request().uri(), traceId, res->{
						if(res.succeeded()) {
							ha.response().end(Json.encodePrettily(res.result()));
						}else {
							ha.response().end(res.cause().toString());
						}
						
					});
				});
				
				router.get("/slot0").handler( ha ->{
					String traceId = randomString();
					fightitwayService.getDietPlanBySlot(ha.request().getParam("email"), 0, traceId, res->{
						if(res.succeeded()) {
							ha.response().end(Json.encodePrettily(res.result()));
						}else {
							ha.response().end(res.cause().toString());
						}
						
					});
				});	
				
				router.get("/removePlan").handler(ha ->{
					String traceId = randomString();
					fightitwayService.removePlan(ha.request().getParam("email"), traceId, r->{
						ha.response().end(r.result().toString());
					});
				});
				router.get("/test").handler( h ->{
					String traceId = randomString();
					CreateOrder createOrder=new CreateOrder();
					createOrder.setAmount(30d);
					createOrder.setEmail("maksood.alam@gmail.com");
					paymentService.createOrder(createOrder, h.request().uri(), traceId, ha->{
						h.response().end(Json.encode(ha.result()));
					});
					
				});
				
				router.get("/getDietByCode").handler( ha ->{
					String traceId = randomString();
					fightitwayService.getDietByCode(ha.request().getParam("code"), traceId, res ->{
						ha.response().end(Json.encodePrettily(res.result()));
					});
					
				});
				router.get("/v2/test").handler(apiHandler::getTest);
				router.get("/authenticate1").handler(h -> {
					String traceId = randomString();
					JsonObject response = new JsonObject();
					String g_token=h.request().getParam("access_token");
					WebClientOptions optionsClient = new WebClientOptions().setSsl(true).setTrustAll(true).setVerifyHost(false);
					optionsClient.setKeepAlive(false);
					WebClient webClient = WebClient.create(vertx.getDelegate(), optionsClient);
					webClient.getAbs("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + g_token + "")
							.send(ha -> {
								
								if (ha.succeeded() && ha.result().statusCode()==200) {
									
									logger.info("##### HA RESULT BODY" + ha.result().body());
									CreateProfileRequest profileRequest=Json.decodeValue(ha.result().body(), CreateProfileRequest.class);
									String token = authProvider.generateToken(new JsonObject().put("access_token",g_token ).put("email", profileRequest.getEmail()),new io.vertx.ext.jwt.JWTOptions().setAlgorithm("ES256"));
									response.put("access_token", token);
									response.put("email", profileRequest.getEmail());
									response.put("code", "0000");
									
									fightitwayService.createProfile(profileRequest, h.request().uri(), traceId, resultHandler->{
										if(resultHandler.succeeded()) {
											logger.info("##### PROFILE CREATED ");
											h.response().end(response.encodePrettily());
										}else {
											h.response().setStatusCode(401);	
										}
									});
								}else {
									response.put("message", "invalid token");
									response.put("code", "401");
									h.response().end(response.encodePrettily());
								}
							});
				});

				HttpServerOptions serverOptions=new HttpServerOptions()
						.removeEnabledSecureTransportProtocol("TLSv1").addEnabledSecureTransportProtocol("TLSv1.2")
						.addEnabledSecureTransportProtocol("TLSv1.3").setSsl(isSecure)
						.setKeyStoreOptions(new JksOptions().setPath(certPath).setPassword(certPassWord)).setPort(port)
						.setHost(host);
				setCrossDomain(router, serverOptions);
				
																	// BATCHES (STARTED)
				
//				Long manualperiod = confJson.getLong("manualperiod", 300000l);
//				Long period = confJson.getLong("period", 86400000l);
//				Long custAnalyticsPeriod = confJson.getLong("custAnalyticsPeriod", 86400000l);
//				Long cacheAnalyticsPPeriod = confJson.getLong("cacheAnalyticsPeriod", 86400000l);
//				Long paymentAnalyticsPeriod = confJson.getLong("paymentAnalyticsPeriod", 86400000l);
				
//				CronExpression cron = null;
//				try {
//					cron = new CronExpression("0 30 13 * * ?");
//				} catch (ParseException e1) {
//					e1.printStackTrace();
//				}
//				Calendar calendar = Calendar.getInstance();
//				calendar.setTime(new Date());
//				Date today = new Date();
//
//				logger.info("##### cron.getNextValidTimeAfter(new Date()) -->> " + cron.getNextValidTimeAfter(today));
				
				
//				Calendar c = Calendar.getInstance();
//				c.setTime(new Date());
//		        c.set(Calendar.HOUR_OF_DAY, 11);
//		        c.set(Calendar.MINUTE, 8);
//		        c.set(Calendar.SECOND, 0);
//		        c.set(Calendar.MILLISECOND, 0);
//				logger.info("##### HOW MANY MILLISECONDS -->> " + c.getTime().getTime());
//				logger.info("##### OPENAPISERVER DATE FROM MILLISECONDS -->> "
//						+ new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date(c.getTime().getTime())));
				
//				vertx.setPeriodic(manualperiod, x -> {
//					logger.info("##### OPENAPISERVER MANUAL CUSTOMER PROFILES BATCH PROCESSING BY SCHEDULER TRIGGER");
//					if (confJson.getBoolean("isSchedulerRequired")) {
//						String traceId = randomString();
//						logger.info("##### OPENAPISERVER " + traceId + " SCHEDULER TRIGGERED.");
//						flightitwayService.updateProfilesManuallyInBulk(traceId, rs -> {
//							if (rs.succeeded()) {
//								logger.info("##### OPENAPISERVER " + traceId + " MANUAL ACTIVITY PERFORMED.");
//							} else {
//								logger.info("##### OPENAPISERVER " + traceId + " MANUAL ACTIVITY FOUND SOME ERROR.");
//							}
//						});
//					}
//				});

				// CUSTOMER PROFILES BATCH PROCESSING BY SCHEDULER TRIGGER
				//vertx.setPeriodic(period, x -> {
//				vertx.setPeriodic(period, x -> {
//					logger.info("##### OPENAPISERVER CUSTOMER PROFILES BATCH PROCESSING BY SCHEDULER TRIGGER");
//					if (confJson.getBoolean("isSchedulerRequired")) {
//						String traceId = randomString();
//						logger.info("##### OPENAPISERVER " + traceId + " SCHEDULER TRIGGERED.");
//						flightitwayService.updateCustomerProfilesInBulk(traceId, rs -> {
//							if (rs.succeeded()) {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY PERFORMED.");
//							} else {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY FOUND SOME ERROR.");
//							}
//						});
//					}
//				});

				// CUSTOMER ANALYTICS BY SCHEDULER TRIGGER
//				vertx.setPeriodic(custAnalyticsPeriod, x -> {
//					//vertx.setPeriodic(c.getTime().getTime(), x -> {
//						logger.info("##### OPENAPISERVER CUSTOMER ANALYTICS BY SCHEDULER TRIGGER");
//					if (confJson.getBoolean("isCustAnalyticsRequired")) {
//						String traceId = randomString();
//						logger.info("##### OPENAPISERVER " + traceId + " CUSTOMER ANALYTICS SCHEDULER TRIGGERED.");
//						flightitwayService.getCustAnalytics(traceId, rs -> {
//							if (rs.succeeded()) {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY PERFORMED.");
//							} else {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY FOUND SOME ERROR.");
//							}
//						});
//					}
//				});

				// CACHE ANALYTICS BY SCHEDULER TRIGGER
//				vertx.setPeriodic(cacheAnalyticsPPeriod, x -> {
//					//vertx.setPeriodic(c.getTime().getTime(), x -> {
//						logger.info("##### OPENAPISERVER CACHE ANALYTICS BY SCHEDULER TRIGGER");
//					if (confJson.getBoolean("isCacheAnalyticsRequired")) {
//						String traceId = randomString();
//						logger.info("##### OPENAPISERVER " + traceId + "CACHE ANALYTICS  SCHEDULER TRIGGERED.");
//						flightitwayService.getDietPlanCacheAnalytics(traceId, rs -> {
//							if (rs.succeeded()) {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY PERFORMED.");
//							} else {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY FOUND SOME ERROR.");
//							}
//						});
//					}
//				});
				
				// PAYMENT ANALYTICS BY SCHEDULER TRIGGER
//				vertx.setPeriodic(paymentAnalyticsPeriod, x -> {
//					//vertx.setPeriodic(c.getTime().getTime(), x -> {
//						logger.info("##### OPENAPISERVER PAYMENT ANALYTICS BY SCHEDULER TRIGGER");
//					if (confJson.getBoolean("isPaymentAnalyticsRequired")) {
//						String traceId = randomString();
//						Integer noOfDays = 0;
//						logger.info("##### OPENAPISERVER " + traceId + " PAYMENT ANALYTICS SCHEDULER TRIGGERED.");
//						flightitwayService.getCustPaymentAnalytics(noOfDays, traceId, rs -> {
//							if (rs.succeeded()) {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY PERFORMED.");
//							} else {
//								logger.info("##### OPENAPISERVER " + traceId + " ACTIVITY FOUND SOME ERROR.");
//							}
//						});
//					}
//				});
				
																	// BATCHES (END)
			} else {
				logger.info("##### ERROR -->> " + ar.cause().toString());
			}
		});

	}
	
 
	private void getToken(RoutingContext context) {
		String email=context.request().getParam("email");
		String token = authProvider.generateToken(new JsonObject().put("email", email).put("deviceId", "21321321"),new io.vertx.ext.jwt.JWTOptions().setAlgorithm("ES256"));
		context.response().end(token);
	}

}
