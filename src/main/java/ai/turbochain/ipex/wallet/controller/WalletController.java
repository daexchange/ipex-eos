package ai.turbochain.ipex.wallet.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.turbochain.ipex.wallet.entity.Coin;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.util.MessageResult;
import io.eblock.eos4j.OfflineSign;
import io.eblock.eos4j.Rpc;
import io.eblock.eos4j.api.exception.ApiException;
import io.eblock.eos4j.api.vo.SignParam;
import io.eblock.eos4j.api.vo.transaction.Transaction;
import okhttp3.RequestBody;
import one.block.eosiojava.interfaces.IRPCProvider;
import one.block.eosiojava.models.rpcProvider.response.GetInfoResponse;

@RestController
@RequestMapping("/rpc")
public class WalletController {
	private Logger logger = LoggerFactory.getLogger(WalletController.class);

	@Autowired
	private Coin coin;
	@Autowired
	private AccountService accountService;
	@Autowired
	private IRPCProvider eosioJavaRpcProviderImpl;
	@Autowired
	private Rpc eos4jRpcProviderImpl;

	@GetMapping("height")
	public MessageResult getHeight() {
		try {
			GetInfoResponse getInfoResponse = eosioJavaRpcProviderImpl.getInfo();
			MessageResult result = new MessageResult(0, "success");
			result.setData(getInfoResponse.getHeadBlockNum().longValue());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "查询失败,error:" + e.getMessage());
		}
	}

	@GetMapping("address/{account}")
	public MessageResult getNewAddress(@PathVariable String account) {
		logger.info("create new address :" + account);
		try {
			accountService.saveOne(account, coin.getWithdrawAddress());
			MessageResult result = new MessageResult(0, "success");
			result.setData(coin.getWithdrawAddress());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "创建账户失败,error:" + e.getMessage());
		}
	}

	@GetMapping("withdraw")
	public MessageResult withdraw(String address, Double amount, String memo) {
		logger.info("withdraw:to={},amount={},memo={}", address, amount, memo);
		String txid = "";
		try {
			// 获取离线签名参数
			SignParam params = eos4jRpcProviderImpl.getOfflineSignParams(60l);
			// 离线签名
			OfflineSign sign = new OfflineSign();
			String txnAmount = new DecimalFormat("#,##0.0000").format(amount)+" EOS";
			String content = sign.transfer(params, coin.getWithdrawWallet(), "eosio.token", coin.getWithdrawAddress(),
					address, txnAmount, memo);
			logger.info(content);
			// 广播交易
			try {
				Transaction tx = eos4jRpcProviderImpl.pushTransaction(content);
				txid = tx.getTransactionId();
			} catch (ApiException ex) {
				logger.error("ApiException" + ex.getError().getCode());
			} catch (Exception e) {
				e.printStackTrace();
			}
			MessageResult result = new MessageResult(0, "success");
			result.setData(txid);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "提现到"+address+"账户失败，error:" + e.getMessage());
		}
	}

	/**
	 * 获取单个地址EOS余额
	 *
	 * @param address
	 * @return
	 */
	@GetMapping("balance/{address}")
	public MessageResult addressBalance(@PathVariable String address) {
		try {
			String GET_CURRENT_BALANCE_REQUEST = "{\n" + "\t\"code\" : \"eosio.token\"\n" + "\t\"account\" : \""
					+ address + "\"\n" + "}";
			@SuppressWarnings("deprecation")
			RequestBody requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
					GET_CURRENT_BALANCE_REQUEST);
			MessageResult result = new MessageResult(0, "success");
			result.setData(eosioJavaRpcProviderImpl.getCurrencyBalance(requestBody));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "查询失败，error:" + e.getMessage());
		}
	}
	
	
	/**
	 * 所有账户余额
	 * 
	 * @return
	 */
	@GetMapping("balance")
	public MessageResult balance() {
		try {
			String GET_CURRENT_BALANCE_REQUEST = "{\n" + "\t\"code\" : \"eosio.token\"\n" + "\t\"account\" : \""
					+ coin.getWithdrawAddress() + "\"\n" + "}";
			@SuppressWarnings("deprecation")
			RequestBody requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
					GET_CURRENT_BALANCE_REQUEST);
			MessageResult result = new MessageResult(0, "success");
			String balance = eosioJavaRpcProviderImpl.getCurrencyBalance(requestBody);
			BigDecimal allBanlanceBigDecimal = new BigDecimal(balance.substring(balance.indexOf("[\\\"") + "[\\\"".length(),
					balance.indexOf(" EOS")));
			result.setData(allBanlanceBigDecimal);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}
}
