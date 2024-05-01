package com.oneness.cp.contract;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;



public class ContractInteraction {

	long timeout = 20*1000;



	public static String airdropAddress = "0x10AB828346c1bBEeaEDc3EdB7be62759B3bb2981";
	public static long chainId = 123666;
	public static String url = "https://rpc.devnet.onenesslabs.io";
	public static String prikey = "";
	public static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);

	static Web3j web3j = Web3j.build(new HttpService(url));

	static DefaultFunctionEncoder functionEncoder = new DefaultFunctionEncoder();
	
	public static  MyRawTransactionManager getMyRawUserTxManager() {
		return getMyRawUserTxManager(10*1000);

	}
	
	
	public static String getData(String user,BigInteger cpAmount) {
		org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
	            Airdrop.FUNC_MINT, 
	            Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(user), 
	            new org.web3j.abi.datatypes.generated.Uint256(cpAmount)), 
	            Collections.<TypeReference<?>>emptyList());
		return functionEncoder.encodeFunction(function);
//		return FunctionEncoder.encode(function);
	}
	

	public static MyRawTransactionManager getMyRawUserTxManager(long timeout) {
		Credentials ownerCredentials = null;

		ownerCredentials = Credentials.create(prikey);

		MyRawTransactionManager transactionManager = new MyRawTransactionManager(web3j, ownerCredentials,
				chainId, 1, timeout);
		return transactionManager;
	}









}
