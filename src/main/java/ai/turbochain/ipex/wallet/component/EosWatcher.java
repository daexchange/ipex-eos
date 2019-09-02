package ai.turbochain.ipex.wallet.component;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import ai.turbochain.ipex.wallet.event.DepositEvent;
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
	@Autowired
	private DepositEvent depositEvent;

	@Override
	public List<Deposit> replayBlock(Long startBlockNumber, Long endBlockNumber) {
		List<Deposit> deposits = new ArrayList<>();
		List<GetBlockResponse> blockListsBlockResponses = new ArrayList<GetBlockResponse>();
		executorService.execute(new Runnable() {
			public void run() {
				for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
					GetBlockRequest request = new GetBlockRequest(blockHeight.toString());
					GetBlockResponse getBlockResponse = null;
					try {
						getBlockResponse = eosioJavaRpcProviderImpl.getBlock(request);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (getBlockResponse != null) {
						blockListsBlockResponses.add(getBlockResponse);
					}
				}
				readBlockTxns(blockListsBlockResponses);
				// readBlockTxnsThread(blockListsBlockResponses);
			}
		});
		return deposits;
	}

	/**
	 * 读取区块交易线程
	 */
	/*
	 * public void readBlockTxnsThread(List<GetBlockResponse>
	 * blockListsBlockResponses) { executorService.execute(new Runnable() { public
	 * void run() { readBlockTxns(blockListsBlockResponses); } }); }
	 */

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void readBlockTxns(List<GetBlockResponse> blockListsBlockResponses) {
		List<Deposit> deposits = new ArrayList<>();
		try {
			for (GetBlockResponse getBlockResponse : blockListsBlockResponses) {

				List<Map> txnsList = getBlockResponse.getTransactions();
				for (int i = 0; i < txnsList.size(); i++) {
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
			deposits.forEach(deposit -> {
				depositEvent.onConfirmed(deposit);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("读取" + blockListsBlockResponses.get(0).getBlockNum() + " 区块到"
				+ blockListsBlockResponses.get(blockListsBlockResponses.size() - 1).getBlockNum() + " 区块数据结束");
		logger.info(Thread.currentThread().getName() + " readBlockTxns 线程结束");
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
