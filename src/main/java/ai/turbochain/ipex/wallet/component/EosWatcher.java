package ai.turbochain.ipex.wallet.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.Coin;
import ai.turbochain.ipex.wallet.entity.Deposit;
import ai.turbochain.ipex.wallet.service.AccountService;
import one.block.eosiojava.interfaces.IRPCProvider;
import one.block.eosiojava.models.rpcProvider.request.GetBlockRequest;
import one.block.eosiojava.models.rpcProvider.response.GetBlockResponse;
import one.block.eosiojava.models.rpcProvider.response.GetInfoResponse;

@Component
public class EosWatcher extends Watcher {
	private Logger logger = LoggerFactory.getLogger(EosWatcher.class);
	@Autowired
	private Coin coin;
	@Autowired
	private AccountService accountService;
	@Autowired
	private IRPCProvider eosioJavaRpcProviderImpl;
	@Autowired
	private ExecutorService executorService;

	@Override
	public List<Deposit> replayBlock(Long startBlockNumber, Long endBlockNumber) {
		List<Deposit> deposits = new ArrayList<>();
		try {
			for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
				GetBlockRequest request = new GetBlockRequest(blockHeight.toString());
				GetBlockResponse getBlockResponse = eosioJavaRpcProviderImpl.getBlock(request);		
				@SuppressWarnings("rawtypes")
				List<Map> txnsList = getBlockResponse.getTransactions();
				for (int i = 0; i < txnsList.size(); i++) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = txnsList.get(i);
					String txnData = map.get("trx").toString();
					String judgingCondition = "to=" + coin.getWithdrawAddress();
					if (txnData.contains(judgingCondition) == true
							&& txnData.indexOf(judgingCondition + ", quantity=") != -1
							&& txnData.indexOf(" EOS, memo=") != -1) {
						String quantity = txnData.substring(txnData.indexOf(judgingCondition + ", quantity=")
								+ (judgingCondition + ", quantity=").length(), txnData.indexOf(" EOS, memo="));
						String memo = txnData.substring(txnData.indexOf(" EOS, memo=") + " EOS, memo=".length(),
								txnData.indexOf("}, hex_data="));
						Account account = accountService.findByName(memo);
						if (account == null) {
							continue;
						}
						Deposit deposit = new Deposit();
						deposit.setTxid(txnData.substring(txnData.indexOf("{id=") + "{id=".length(),
								txnData.indexOf(", signatures=[SIG_K1")));
						deposit.setAmount(new BigDecimal(quantity));
						deposit.setAddress(coin.getWithdrawAddress());
						deposit.setBlockHeight(getBlockResponse.getBlockNum().longValue());
						deposits.add(deposit);
						logger.info("receive {} EOS", quantity);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deposits;
	}

	/**
	 * 读取区块交易线程
	 */
	public void readBlockTxnsThread() {
		executorService.execute(new Runnable() {
			public void run() {
				
			}
		});
	}
	
	public void readBlockTxns() {
		List<Deposit> deposits = new ArrayList<>();
		try {
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Long getNetworkBlockHeight() {
		try {
			GetInfoResponse getInfoResponse = eosioJavaRpcProviderImpl.getInfo();
			return getInfoResponse.getHeadBlockNum().longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return 0L;
		}
	}
}
