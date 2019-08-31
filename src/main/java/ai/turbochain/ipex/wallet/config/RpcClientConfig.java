package ai.turbochain.ipex.wallet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.eblock.eos4j.Rpc;
import one.block.eosiojava.interfaces.IRPCProvider;
import one.block.eosiojavarpcprovider.error.EosioJavaRpcProviderInitializerError;
import one.block.eosiojavarpcprovider.implementations.EosioJavaRpcProviderImpl;

/**
 * 初始化RPC客户端
 */
@Configuration
public class RpcClientConfig {
    private Logger logger = LoggerFactory.getLogger(RpcClientConfig.class);

    /**
     * osp-eos RPC客户端
     * @param uri
     * @return
     */
    @Bean
    public IRPCProvider eosioJavaRpcProviderImpl(@Value("${coin.rpc}") String uri){
        try {
            logger.info("uri={}",uri);
            IRPCProvider eosioJavaRpcProviderImpl = new EosioJavaRpcProviderImpl(uri);
            logger.info("=============================");
            logger.info("client={}",eosioJavaRpcProviderImpl);
            logger.info("=============================");
            return eosioJavaRpcProviderImpl;
        } catch (EosioJavaRpcProviderInitializerError e) {
            logger.info("init osp-eos RPC客户端 failed");
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * eos4j RPC客户端
     * @param uri
     * @return
     */
    @Bean
    public Rpc eos4jRpcProviderImpl(@Value("${coin.rpc}") String uri){
        try {
            logger.info("uri={}",uri);
            Rpc eos4jRpcProviderImpl = new Rpc(uri);
            logger.info("=============================");
            logger.info("client={}",eos4jRpcProviderImpl);
            logger.info("=============================");
            return eos4jRpcProviderImpl;
        } catch (Exception e) {
        	logger.info("init eos4j RPC客户端 failed");
            e.printStackTrace();
            return null;
        }
    }
    
}
