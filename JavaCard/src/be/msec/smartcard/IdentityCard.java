package be.msec.smartcard;

//import java.nio.ByteBuffer;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
//import javacard.security.Signature;
import javacard.security.RandomData;



public class IdentityCard extends Applet {
	private final static byte IDENTITY_CARD_CLA =(byte)0x80;
	
	private final short SHORTZERO = 0;
	
	private final short ISSUER = 0;
	private final short SUBJECT = 1;
	private final short PUBLIC_KEY_MODULUS = 2;
	private final short PUBLIC_KEY_EXPONENT = 3;
	private final short START_DATE = 4;
	private final short END_DATE = 5;
	private final short SIGNATURE = 6;
	
	private static final byte VALIDATE_PIN_INS = 0x22;
	private static final byte GET_SERIAL_INS = 0x24;
	private static final byte SIGN_DATA = 0x26;
	private static final byte ECHO = 0x28;
	private static final byte VALIDATE_TIME = 0x30;
	private static final byte VERIFY_TIME_SIG = 0x32;
	private static final byte AUTHENTICATE_SP = 0x34;
	private static final byte AUTHENTICATE_SP_STEP = 0x36;
	private static final byte END_AUTH = 0x38;
	private static final byte AUTHENTICATE_CARD = 0x40;
	private static final byte QUERY_ATTRIBUTES = 0x42;
	
	private static final short ISSUER_LEN = 16; 
	private static final short SUBJECT_LEN = 16;
	private static final short DATE_LEN = 8; 
	private static final short EXPONENT_LEN = 3; 
	private static final short MODULUS_LEN = 64; 

	private static final short SIGN_LEN = 64; 
	
	private static byte auth = (byte) 0x00;

	private static final short SIZE_OF_CHALLENGE = 2;
	private static final short SIZE_OF_AES = 16;
	private static final short SIZE_OF_PADDED_CHALLENGE = 16;
	private static final short SIZE_OF_AUTH = 4;
	private static final short SIZE_OF_UNIQUE_KEY = 4;
	private static final short SIZE_OF_CERT = ISSUER_LEN + SUBJECT_LEN + 2*DATE_LEN + EXPONENT_LEN + MODULUS_LEN + SIGN_LEN;
	
	private static final short SIZE_OF_DATE = 8;
	private static final short SIZE_OF_INT = 4;
	
	
	private final static byte PIN_TRY_LIMIT =(byte)0x03;
	private final static byte PIN_SIZE =(byte)0x04;
	
	private final static short SW_VERIFICATION_FAILED = 0x6300;
	private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;
	private final static short SW_TIME_UPDATE_FAILED = 0x6302;
	private final static short SW_SP_NOT_AUTH = 0x6303;
	private final static short SW_TIME_SIGNATURE_VERIFICATION_FAILED = 0x6304;
	private final static short SW_CERT_VERIFICATIONR_OR_VALIDATION_FAILED = 0x6305;
	private final static short SW_WRONG_CHALLENGE = 0x6306;
	private final static short SW_WRONG_REQUEST = 0x6307;
	private final static short SW_WRONG_PIN = 0x6308;

	private AESKey symKey = null;
	private byte[] arrayOfOne = new byte[1];
	private byte[] userUniqueKey = new byte[SIZE_OF_UNIQUE_KEY];
	private byte[] emptyResponse = new byte[0];
	private byte[] dateBuffer = new byte[SIZE_OF_DATE];
	private byte[] diffDateBuffer = new byte[SIZE_OF_DATE];
	private byte[] pinBuffer = new byte[PIN_SIZE];
	private byte[] issuerBuffer = new byte[ISSUER_LEN];
	private byte[] dataFornym = new byte[SIZE_OF_UNIQUE_KEY + SUBJECT_LEN];
	private byte[] subjectBuffer = new byte[SUBJECT_LEN];
	private byte[] hashedArray = new byte[32];
	private byte[] authenticatedSPCertificate = new byte[SIZE_OF_CERT];
	private byte[] signatureBytes = new byte[SIGN_LEN];
	private byte[] aesEncryptBytes = new byte[SIZE_OF_AES];
	private byte[] symKeyBytes = new byte[SIZE_OF_AES];
	private byte[] challenge = new byte[SIZE_OF_CHALLENGE];
	private byte[] paddedChallenge = new byte[SIZE_OF_PADDED_CHALLENGE];
	private byte[] concatChallengeAuth = new byte[SIZE_OF_CHALLENGE + SIZE_OF_AUTH];
	private byte[] certificateWithoutSign = new byte [SIZE_OF_CERT - SIGN_LEN];
	private byte[] paddedEncryptedData = new byte[64];
	private byte[] encryptedChallenge = new byte[32];
	private byte[] dataToEncrypt = new byte[SIZE_OF_CHALLENGE + SUBJECT_LEN]; 
	private byte[] lenAndEncryptedPaddedChallenge = new byte[SIZE_OF_INT + SIZE_OF_PADDED_CHALLENGE];
	private byte[] lenAndpaddedChallenge = new byte[SIZE_OF_INT + SIZE_OF_PADDED_CHALLENGE];
	
	private byte[] paddedDataToEncrypt_1 = new byte[32];
	private byte[] randomGeneratedData = new byte[128];

	private byte[] response = new byte[4 + 4 + paddedEncryptedData.length +  encryptedChallenge.length];
	private byte[] certificateAndSignature = new byte[SIGN_LEN + SIZE_OF_CERT];
	private short  sizeToAdd = (short) (16 - (certificateAndSignature.length%16));
	private byte[] paddedDataToEncrypt = new byte[certificateAndSignature.length + sizeToAdd];
	

	private byte[] encryptedCertificateAndSignature = new byte[certificateAndSignature.length + sizeToAdd];
	private byte[] keybytes = new byte[16];

	byte[] exponent = new byte[EXPONENT_LEN];
	byte[] validFrom = new byte[DATE_LEN];
	byte[] validUntil = new byte[DATE_LEN];
	byte[] signature = new byte[SIGN_LEN];
	byte[] issuer = new byte[ISSUER_LEN];
	byte[] subject = new byte[SUBJECT_LEN];
	byte[] modulus = new byte[MODULUS_LEN+1];
	byte[] croppedModulus = new byte[MODULUS_LEN];
	
	
	
	private byte[] serial = new byte[]{0x30, 0x35, 0x37, 0x36, 0x39, 0x30, 0x31, 0x05};
	private byte[] javacardPrivateExponent = {76,-67,-83,101,-57,121,-108,65,10,-71,-126,-44,27,84,100,-96,-109,-6,-47,-42,86,25,-122,-30,-9,-95,-116,115,18,51,-88,69,-5,-83,-72,-97,85,-18,18,91,-99,65,-101,-19,-98,50,-125,-19,124,-55,-59,-37,-47,-26,4,-87,109,91,117,77,-43,124,110,65};
	private byte[] javacardModulus = {-119,-11,15,121,24,-88,70,-8,104,-83,31,12,-96,-128,-120,-117,-73,-100,126,-95,-69,-23,-126,6,-98,86,-32,-9,101,56,-5,47,-96,-49,1,-80,25,-127,80,5,-76,-4,-19,-3,107,99,-90,-57,79,64,-95,-110,-76,-60,77,91,-62,30,-121,-109,115,-32,-64,99};
	
	//will need to be changed to the new kind of certificate
	private byte[] javacardCert = {106,97,118,97,99,97,114,100,0,0,0,0,0,0,0,0,106,97,118,97,99,97,114,100,0,0,0,0,0,0,0,0,-119,-11,15,121,24,-88,70,-8,104,-83,31,12,-96,-128,-120,-117,-73,-100,126,-95,-69,-23,-126,6,-98,86,-32,-9,101,56,-5,47,-96,-49,1,-80,25,-127,80,5,-76,-4,-19,-3,107,99,-90,-57,79,64,-95,-110,-76,-60,77,91,-62,30,-121,-109,115,-32,-64,99,1,0,1,0,0,7,-31,1,25,9,44,0,0,7,-30,1,25,9,44,127,-26,78,23,107,7,-38,71,-43,-8,-49,-59,-89,-92,59,-34,-13,-12,54,-70,81,60,-108,51,1,-31,-52,-76,119,-120,-43,114,-51,89,90,61,-68,-78,-87,-90,-103,80,-98,80,95,103,21,71,-16,18,10,30,-87,50,2,83,-42,-65,-60,-105,75,-21,11,-45};

	private byte[] mainCAPublicExponent = {1,0,1};
	private byte[] mainCAPublicModulus = {-111,103,-6,88,-39,13,27,-42,85,-123,-123,-92,101,-57,-34,83,42,-118,-101,115,38,22,-113,-108,-21,97,-21,99,-18,-77,54,58,32,115,-47,-80,-71,53,43,-3,81,88,114,-25,-114,125,-12,-53,108,25,-49,37,15,66,20,8,52,-99,-49,-79,23,81,50,23};

	private byte[] authText = {65,117,116,104};
	
	private byte[] govTimePublicExponent = {1,0,1};
	private byte[] govTimePublicModulus = {-122,-6,-74,-13,93,84,85,-61,-39,4,-102,-7,82,43,-67,-2,-63,-65,-69,100,51,-106,4,94,63,7,-67,61,127,-16,-59,-95,34,-49,14,14,94,44,81,-36,94,26,-45,46,-100,-40,-30,-55,-69,40,124,-3,0,-2,-84,-97,0,-87,77,44,-29,-20,-80,49};
	
	private byte[] EGOV_BYTES =    {101,103,111,118,0,0,0,0,0,0,0,0,0,0,0,0};
	private byte[] HEALTH_BYTES =  {104,101,97,108,116,104,0,0,0,0,0,0,0,0,0,0};
	private byte[] SOCNET_BYTES =  {115,111,99,110,101,116,0,0,0,0,0,0,0,0,0,0};
	private byte[] DEFAULT_BYTES = {100,101,102,97,117,108,116,0,0,0,0,0,0,0,0,0};
	
	private byte[] ivBytes = {48,48,48,48,49,49,49,49,50,50,50,50,51,51,51,51};
	
	
	private byte[] lastValidationTimeBytes = {0,0,1,69,-73,-28,62,-64};
	private byte[] millisInADay = {0,0,0,0,5,38,92,0};
	
	
	private OwnerPIN pin;
	private byte[] storage = new byte[]{0x30, 0x35, 0x37, 0x36, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04};
	private byte[] bigStorage = new byte[1024];
	private byte[] intInBytes = new byte[]{0x00,0x00,0x00,0x00};
	private byte[] fourBytes = new byte[4];
	
	private short authStep = 0;
	
	private byte[] nym = new byte[32];
	private byte[] name = new byte[32];
	private byte[] address = new byte[48];
	private byte[] country = new byte[2];
	private byte[] birthDate = new byte[6];
	private byte donor;
	private byte age;
	private byte gender;
	byte[] picture = new byte[]{0x30, 0x35, 0x37, 0x36, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04};
// max size byte array to release attributes
	private byte[] responseToAttributesReleasing = new byte[32 + 32 + 48 + 2 + 6 + 1 + 1 + 1 + 16];
	private byte[] paddedResponseToAttributesReleasing = new byte[32 + 32 + 48 + 2 + 6 + 1 + 1 + 1 + 16 + 5];//Multiple of 16 for encryption
	private byte[] encryptedPaddedResponseToAttributesReleasing = new byte[32 + 32 + 48 + 2 + 6 + 1 + 1 + 1 + 16 + 5];//Multiple of 16 for encryption result
	
	private final byte NYM_INDEX = 9;
	private final byte NAME_INDEX = 1;
	private final byte ADDRESS_INDEX = 2;
	private final byte COUNTRY_INDEX = 3;
	private final byte BIRTHDATE_INDEX = 4;
	private final byte DONOR_INDEX = 5;
	private final byte AGE_INDEX = 6;
	private final byte GENDER_INDEX = 7;
	private final byte PICTURE_INDEX = 8;

	private byte[] canEgovAccess = {NYM_INDEX, NAME_INDEX, ADDRESS_INDEX, COUNTRY_INDEX, BIRTHDATE_INDEX, AGE_INDEX, GENDER_INDEX};
	private byte[] canSocNetAccess = {NYM_INDEX, NAME_INDEX, COUNTRY_INDEX, AGE_INDEX, GENDER_INDEX, PICTURE_INDEX};
	private byte[] canDefaultAccess = {NYM_INDEX, AGE_INDEX};
	private byte[] canHealthAccess = {NYM_INDEX, NAME_INDEX, DONOR_INDEX, AGE_INDEX, GENDER_INDEX, PICTURE_INDEX};
	
	private byte[] currentQuery = new byte[9];
	
	
	private IdentityCard() {
		/*
		 * During instantiation of the applet, all objects are created.
		 * In this example, this is the 'pin' object.
		 */
		pin = new OwnerPIN(PIN_TRY_LIMIT,PIN_SIZE);
		pin.update(new byte[]{0x01,0x02,0x03,0x04},(short) 0, PIN_SIZE);
		byte[] unpaddedName = {74,101,97,110,32,68,117,112,111,110,116}; // Jean Dupont
		Util.arrayCopy(unpaddedName, SHORTZERO, name, SHORTZERO, (short) Math.min(name.length, unpaddedName.length));
		byte[] unpaddedAddress = {50,51,32,82,111,97,100,108,97,110,101,32,84,101,120,97,115};//"23 Roadlane Texas"
		Util.arrayCopy(unpaddedAddress, SHORTZERO, address, SHORTZERO, (short) Math.min(address.length, unpaddedAddress.length));
		
		byte[] unpaddedCountry = {66,69};
		Util.arrayCopy(unpaddedCountry, SHORTZERO, country, SHORTZERO, (short) Math.min(country.length, unpaddedCountry.length));

		
		 javacard.security.RandomData randomizer = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
		try {
			//only pseudo_random available on the card
			randomizer.generateData(userUniqueKey, (short) 0, (short) userUniqueKey.length);
			System.out.println("RANDOM USER UNIQUE KEY:\n" + javax.xml.bind.DatatypeConverter.printHexBinary(userUniqueKey));
		}
		catch( Exception e){
			
		}
		byte[] yearBytes = {0,0,7,-83};
		byte[] timeBytes = new byte[6];
		System.arraycopy(yearBytes, 0, timeBytes, 0, 4);
		timeBytes[4] = 4;
		timeBytes[5] = 23;
		Util.arrayCopy(timeBytes, SHORTZERO, birthDate, SHORTZERO, (short)birthDate.length);
		
		// 0 is Male, 1 is female. Other numbers can also be used for other genders.
		gender = 1;
		// 0 is no donor, 1 is full donor. Other numbers (till 255) for special kinds of donor
		donor = 1;
		age = 53;

		
		/*
		 * This method registers the applet with the JCRE on the card.
		 */
		register();
	}

	/*
	 * This method is called by the JCRE when installing the applet on the card.
	 */
	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new IdentityCard();
	}
    /**
    * Subtract two unsigned integers represented in array A and B. The sum stored in
    * array C
    * From https://stackoverflow.com/questions/36518553/javacard-applet-to-subtract-two-hexadecimal-array-of-byte
    * @param A the left operand
    * @param AOff the starting position in array A.
    * @param B the right operand.
    * @param BOff the starting position in array B.
    * @param C the result of (A-B)
    * @param COff the starting position in array C.
    * @param len the number of bytes in the operands as well in the computed
    * result. this parameter can not be a negative number.
    * @return false if the result underflows. if underflows occurs, the sum would
    * be the mathematical result of A + ~B + 1.
    * @throws ArrayOuutOfBoundException if  array access out of bound.
    */
   public static boolean substract(byte[] A, byte AOff, byte[] B, byte BOff, byte[] C, byte COff, byte len) {
       byte borrow = 0;
       short result;

       for (len = (byte) (len - 1); len >= 0; len--) {
           // subtract one unsigned byte from the other.
           // also subtract the borrow from the previous byte computation.
           result = (short) (getUnsignedByte(A, AOff, len) - getUnsignedByte(B, BOff, len) - borrow);
           // need to borrow?
           if (result < 0) {
               borrow = 1;
               result = (short) (result + 0x100);
           } else {
               borrow = 0;
           }
           // store the result in C
           C[(byte) (len + COff)] = (byte) result;
       }
       // is the result underflow?
       if (borrow == 1) {
           return false;
       }
       return true;

   }
   
   private static byte arrayCompare(byte[] A, short AOff, byte[] B, short BOff, short len){
	   if (A.length == B.length){
		   for (int i = 0; i < len; i++) {
			   System.out.println(A[i] + " byte compared to " + B[i]);
			   short Abyte = (short) (A[i] & 0xFF);
			   short Bbyte = (short) (B[i] & 0xFF);
			   System.out.println(Abyte + " compared to " + Bbyte);
			   if (Abyte>Bbyte) {
				   return 1;
			   } else if (Bbyte>Abyte){
				   return -1;
			   }
		   }
		   return 0;
	   }else{
		   return 2;
	   }
   }
   
   private static short getUnsignedByte(byte[] A, byte AOff, byte count) {
       return (short) (A[(short) (count + AOff)] & 0x00FF);
   }
	
	private void validate_time(APDU apdu){
		System.out.println("Validate");
		byte[] buffer = apdu.getBuffer();
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short START = 0;
		Util.arrayCopy(buffer, START, dateBuffer, START, (short)8);
		short readCount = apdu.setIncomingAndReceive();
		
		substract(dateBuffer, (byte) 0,lastValidationTimeBytes, (byte) 0, diffDateBuffer, (byte) 0, (byte) SIZE_OF_DATE);
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(diffDateBuffer));
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(millisInADay));
		byte[] zero = {(byte)0x00};
		byte[] one = {(byte)0x01};
		System.out.println(arrayCompare(one, SHORTZERO, zero, SHORTZERO, (short)1));
		
		if(arrayCompare(diffDateBuffer, SHORTZERO, millisInADay, SHORTZERO, SIZE_OF_DATE) == 1){
			System.out.println("NEEDS REFRESH");
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)1);
			Util.setShort(buffer,(short) 0, (short) 1);
			apdu.sendBytes((short) 0x00,(short)1);
			
		}
		else {
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)1);
			Util.setShort(buffer,(short) 0, (short) 1);
			apdu.sendBytes((short) 0x00,(short)0);
			
		}
	}
	
	
	private void verify_time_signature(APDU apdu) {
		//4 bytes for len, 8 for date, rest for sign
		System.out.println("VERIF");
		byte[] buffer = apdu.getBuffer();
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short START = 0;
		Util.arrayCopy(buffer, START, bigStorage, START, (short)8);
		short readCount = apdu.setIncomingAndReceive();
		short i = 0;
		while ( bytesLeft > 0){
			
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, bigStorage, i, readCount);
			bytesLeft -= readCount;
			i+=readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
        int len = bigStorage[0] << 24 | (bigStorage[1] & 0xFF) << 16 | (bigStorage[2] & 0xFF) << 8 | (bigStorage[3] & 0xFF);
                
        Util.arrayCopy(bigStorage, (short) SIZE_OF_INT, dateBuffer, (short) 0, (short)(SIZE_OF_DATE));
        Util.arrayCopy(bigStorage, (short) (SIZE_OF_INT + SIZE_OF_DATE), signatureBytes, (short) 0, (short)(len - SIZE_OF_INT - SIZE_OF_DATE));

		javacard.security.RSAPublicKey timestampPubKey = (javacard.security.RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);

        timestampPubKey.setExponent(govTimePublicExponent, (short) 0, (short) govTimePublicExponent.length);
        timestampPubKey.setModulus(govTimePublicModulus, (short) 0, (short) govTimePublicModulus.length);

		
        javacard.security.Signature signEngine = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_RSA_SHA_PKCS1, false);
		signEngine.init( timestampPubKey, javacard.security.Signature.MODE_VERIFY);
		
		javacard.security.MessageDigest md = javacard.security.MessageDigest.getInstance(javacard.security.MessageDigest.ALG_SHA_256, false);
		md.reset();
		md.doFinal(dateBuffer, (short) 0,(short)dateBuffer.length, hashedArray, (short) 0);
		boolean verifies = signEngine.verify(hashedArray, (short) 0, (short) hashedArray.length, signatureBytes, (short) 0, (short)signatureBytes.length);
		if (! verifies){
			ISOException.throwIt(SW_TIME_SIGNATURE_VERIFICATION_FAILED);
		}
		

		short response = 1;	
		System.out.println("COMPARISON: "+arrayCompare(dateBuffer, SHORTZERO,lastValidationTimeBytes, SHORTZERO, SIZE_OF_DATE));
		if(!verifies || !(arrayCompare(dateBuffer, SHORTZERO,lastValidationTimeBytes, SHORTZERO, SIZE_OF_DATE) == 1)){ //timeDate.getTime() > lastValidationDate.getTime())){
			response = 0;
		}
		else{
			lastValidationTimeBytes = dateBuffer;
		}
		System.out.println(response);
		arrayOfOne[0] = (byte) response;
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)arrayOfOne.length);
		apdu.sendBytesLong(arrayOfOne,(short)0,(short)arrayOfOne.length);


		
	}

	
	private void auth_step(APDU apdu) {
		if (authStep == 0) {
			Util.arrayFillNonAtomic(bigStorage, SHORTZERO, (short) bigStorage.length, (byte) 0);
		}
		byte[] buffer = apdu.getBuffer();
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short START = 0;
		Util.arrayCopy(buffer, START, storage, START, (short)8);
		short readCount = apdu.setIncomingAndReceive();
		short i = (short) (250*authStep);
		while ( bytesLeft > 0){
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, bigStorage, i, readCount);
			bytesLeft -= readCount;
			i+=readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		
		
		authStep += 1;
		apdu.setOutgoing();
		System.out.println((short)buffer.length);
		apdu.setOutgoingLength((short)buffer.length);
		apdu.sendBytesLong(buffer,(short)0,(short)buffer.length);    
		
	}
	private byte[] intToByte(int value){
	    intInBytes[0] = (byte)(value >>> 24);
	    intInBytes[1] = (byte)(value >>> 16);
	    intInBytes[2] = (byte)(value >>> 8);
	    intInBytes[3] = (byte)value;
	    return intInBytes;
	}
	
	
	private void authenticate_sp(APDU apdu){
		System.out.println("AUTH_SP");
		
		authStep = 0;
		byte[] buffer = apdu.getBuffer();
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short START = 0;
		Util.arrayCopy(buffer, START, storage, START, (short)8);
		short readCount = apdu.setIncomingAndReceive();
		short i = (short) (250*authStep);
		while ( bytesLeft > 0){
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, storage, i, readCount);
			bytesLeft -= readCount;
			i+=readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		
		modulus[0] = 0;

		//Store for later
		Util.arrayCopy(bigStorage, SHORTZERO, authenticatedSPCertificate, SHORTZERO, (short) authenticatedSPCertificate.length);
		
		Util.arrayCopy(bigStorage, (short) 0x00, issuer,(short) 0x00, ISSUER_LEN);
		Util.arrayCopy(bigStorage, (short) ISSUER_LEN, subject,(short) 0x00, SUBJECT_LEN);
		Util.arrayCopy(bigStorage, (short) (SUBJECT_LEN + ISSUER_LEN), modulus,(short) 0x01, MODULUS_LEN);
		Util.arrayCopy(bigStorage, (short) (SUBJECT_LEN + ISSUER_LEN + MODULUS_LEN), exponent,(short) 0x00, EXPONENT_LEN);
		Util.arrayCopy(bigStorage, (short) (SUBJECT_LEN + ISSUER_LEN + MODULUS_LEN + EXPONENT_LEN), validFrom,(short) 0x00, DATE_LEN);
		Util.arrayCopy(bigStorage, (short) (SUBJECT_LEN + ISSUER_LEN + MODULUS_LEN + EXPONENT_LEN + DATE_LEN), validUntil,(short) 0x00, DATE_LEN);
		Util.arrayCopy(bigStorage, (short) (SUBJECT_LEN + ISSUER_LEN + MODULUS_LEN + EXPONENT_LEN + DATE_LEN + DATE_LEN), signature,(short) 0x00, SIGN_LEN);
		
		Util.arrayFillNonAtomic(certificateWithoutSign, SHORTZERO, (short) certificateWithoutSign.length, (byte) 0);
		Util.arrayCopy(bigStorage, (short) 0, certificateWithoutSign, (short)0,(short) (SUBJECT_LEN + ISSUER_LEN + MODULUS_LEN + EXPONENT_LEN + DATE_LEN + DATE_LEN));
		
		
		boolean verified = false;
		boolean valid = false;
		javacard.security.RSAPublicKey mainCaPublicKey = null;
		// now we need to verify if the certificate is correct
		mainCaPublicKey = (javacard.security.RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
		mainCaPublicKey.setExponent(mainCAPublicExponent, (short) 0, (short) mainCAPublicExponent.length);
		mainCaPublicKey.setModulus(mainCAPublicModulus, (short) 0, (short) mainCAPublicModulus.length);
		javacard.security.Signature SPcheck = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_RSA_SHA_PKCS1, false);
		SPcheck.init( mainCaPublicKey, javacard.security.Signature.MODE_VERIFY);
		verified = SPcheck.verify(certificateWithoutSign, (short) 0, (short) certificateWithoutSign.length, signature, (short) 0, (short)signature.length);

		System.out.println("verified" + verified);
		

		if((arrayCompare(lastValidationTimeBytes, SHORTZERO, validFrom, SHORTZERO, SIZE_OF_DATE) == 1) && (arrayCompare(validUntil, SHORTZERO, lastValidationTimeBytes, SHORTZERO, SIZE_OF_DATE) == 1)) {
		   valid = true;
		}
		System.out.println("valid" + valid);

		if (!(verified && valid)){
			ISOException.throwIt(SW_CERT_VERIFICATIONR_OR_VALIDATION_FAILED);
			
		}
		System.out.println("HERE");
		//otherwise we generate a new symmetric key
		 AESKey key = null;
		 javacard.security.RandomData randomizer = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);
		try {
			System.out.println("beforeSymKey");
			//only pseudo_random available on the card
			randomizer.generateData(randomGeneratedData, (short) 0, (short) randomGeneratedData.length);
			key = (AESKey) javacard.security.KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
			key.setKey(randomGeneratedData, (short) 0);
			symKey = key;
			symKey.getKey(symKeyBytes, (short)0);
		} catch (Exception e) {
			System.out.println("NoSuchAlgorithmException");
		}
		
		try {
			javacardx.crypto.Cipher rsaenc = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_RSA_PKCS1, false);
			javacard.security.RSAPublicKey spPublicKey = (javacard.security.RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_512, false);
			spPublicKey.setExponent(exponent, (short) 0, (short) exponent.length);
			//Modulus only needs to be 64 bytes
			Util.arrayCopy(modulus, (short) 1, croppedModulus,(short)  0,(short)  (croppedModulus.length));
			spPublicKey.setModulus(croppedModulus, (short) 0, (short) croppedModulus.length);
			rsaenc.init(spPublicKey, javacardx.crypto.Cipher.MODE_ENCRYPT);
			
			rsaenc.doFinal(symKeyBytes, (short) 0, (short) symKeyBytes.length, paddedEncryptedData, (short) 0);
			System.out.println("Encryption is ok");
			
			//we have to generate a challenge 
			
			randomizer.generateData(challenge, (short) 0, (short) challenge.length);

	// step 2.7
		    javacardx.crypto.Cipher cipher = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		    System.out.println(symKey.getSize());
		    cipher.init(symKey, javacardx.crypto.Cipher.MODE_ENCRYPT, ivBytes, (short)0, (short)ivBytes.length);
		    

//		    System.out.println("Challenge: " + javax.xml.bind.DatatypeConverter.printHexBinary(challenge));
			// data that needs to be encrypted
			
			System.out.println("SIZE OF DATA TO ENCRYPT: " + dataToEncrypt.length);
			Util.arrayCopy(challenge,(short) 0, dataToEncrypt,(short) 0, SIZE_OF_CHALLENGE);
			Util.arrayCopy(subject,(short)0, dataToEncrypt, SIZE_OF_CHALLENGE, SUBJECT_LEN);

			Util.arrayCopy(dataToEncrypt,(short) 0, paddedDataToEncrypt_1,(short) 0, (short)dataToEncrypt.length);
			Util.arrayFillNonAtomic(paddedDataToEncrypt_1, (short)dataToEncrypt.length,(short) (paddedDataToEncrypt_1.length-dataToEncrypt.length), (byte) 0);

			
			System.out.println(paddedDataToEncrypt_1.length);
		    cipher.doFinal(paddedDataToEncrypt_1, (short) 0, (short) paddedDataToEncrypt_1.length, encryptedChallenge, (short) 0);
		    
		    
//			byte[] encryptedChallenge = aesenc.doFinal(dataToEncrypt);
			Util.arrayCopy(intToByte(paddedEncryptedData.length), (short) 0, response, (short) 0, (short) 4);
			Util.arrayCopy(intToByte(encryptedChallenge.length), (short) 0, response, (short) 4, (short) 4);
			Util.arrayCopy(paddedEncryptedData, (short) 0, response, (short) 8,(short) paddedEncryptedData.length);
			Util.arrayCopy(encryptedChallenge, (short) 0, response, (short) (paddedEncryptedData.length +8),(short) encryptedChallenge.length);
			System.out.println(encryptedChallenge.length);
			System.out.println("END OF AUTH 1");
			
		} catch (Exception e) {
			System.out.println("CryptoException");
//		} catch (NoSuchPaddingException e) {
//			System.out.println("NoSuchPaddingException");
////		} catch (InvalidKeyException e) {
////			System.out.println("InvalidKeyException");
//		} catch (IllegalBlockSizeException e) {
//			System.out.println("IllegalBlockSizeException");
//		} catch (BadPaddingException e) {
//			System.out.println("BadPaddingException");
////		} catch (InvalidAlgorithmParameterException e) {
////			System.out.println("InvalidAlgorithmParameterException");
//		} catch (UnsupportedEncodingException e) {
//			System.out.println("UnsupportedEncodingException");
//		} catch (InvalidKeySpecException e) {
//			System.out.println("InvalidKeySpecException");

		}
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)response.length);
		apdu.sendBytesLong(response,(short)0,(short)response.length);    
		
	}
	
	
	/*
	 * If no tries are remaining, the applet refuses selection.
	 * The card can, therefore, no longer be used for identification.
	 */
	public boolean select() {
		if (pin.getTriesRemaining()==0)
			return false;
		
		return true;
	}

	/*
	 * This method is called when the applet is selected and an APDU arrives.
	 */
	public void process(APDU apdu) throws ISOException {
		//A reference to the buffer, where the APDU data is stored, is retrieved.
		byte[] buffer = apdu.getBuffer();
		
		//If the APDU selects the applet, no further processing is required.
		if(this.selectingApplet())
			return;
		
		//Check whether the indicated class of instructions is compatible with this applet.
		if (buffer[ISO7816.OFFSET_CLA] != IDENTITY_CARD_CLA){ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);};
		//A switch statement is used to select a method depending on the instruction
		switch(buffer[ISO7816.OFFSET_INS]){
		case VALIDATE_PIN_INS:
			
			validatePIN(apdu);
			break;
		case GET_SERIAL_INS:
			getSerial(apdu);
			break;
		case SIGN_DATA:
			sign_data(apdu);
			break;
		case ECHO:
			echo(apdu);
			break;
		case VALIDATE_TIME:
			validate_time(apdu);
			break;
		case VERIFY_TIME_SIG:
			try {
				verify_time_signature(apdu);
			} catch (Exception e) {}
			break;
		case AUTHENTICATE_SP: 
			authenticate_sp(apdu);
			break;
		case AUTHENTICATE_SP_STEP:
			auth_step(apdu);
			break;
		case END_AUTH:
			end_auth(apdu);
			break;
		case AUTHENTICATE_CARD:
			auth_card(apdu);
			break;
		case QUERY_ATTRIBUTES:
			release_attributes(apdu);
			break;
			
		//If no matching instructions are found it is indicated in the status word of the response.
		//This can be done by using this method. As an argument a short is given that indicates
		//the type of warning. There are several predefined warnings in the 'ISO7816' class.
		default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	

	private void release_attributes(APDU apdu) {
	//step 4.2
		byte[] buffer = apdu.getBuffer();
		byte queryLen = buffer[ISO7816.OFFSET_CDATA];
		System.out.println(queryLen);
		System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(buffer));
		Util.arrayFillNonAtomic(bigStorage, SHORTZERO,(short) bigStorage.length, (byte) 0);
		Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA+1), bigStorage, SHORTZERO,(short) queryLen);
		Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA+1 + queryLen), pinBuffer, SHORTZERO, PIN_SIZE);
	// step 4.3
		if (pin.check(pinBuffer, SHORTZERO,PIN_SIZE)==false){
			System.out.println("INVALID PIN");
			ISOException.throwIt(SW_WRONG_PIN);
		}
	// step 4.4
		else if (auth == (byte)0x00){
			System.out.println("NOT AUTH");
			ISOException.throwIt(SW_SP_NOT_AUTH);
		}
	// step 4.5
		else{
			System.out.println("4.5");
			Util.arrayCopy(authenticatedSPCertificate, SHORTZERO, issuerBuffer, SHORTZERO, ISSUER_LEN);
			Util.arrayCopy(authenticatedSPCertificate, ISSUER_LEN, subjectBuffer, SHORTZERO, SUBJECT_LEN);
			
			byte[] accessibleFields = null;
			if (Util.arrayCompare(issuerBuffer, SHORTZERO, EGOV_BYTES, SHORTZERO, (short) EGOV_BYTES.length) == 0) {
				accessibleFields = canEgovAccess;
			} 
			else if(Util.arrayCompare(issuerBuffer, SHORTZERO, SOCNET_BYTES, SHORTZERO, (short) SOCNET_BYTES.length) == 0){
				accessibleFields = canSocNetAccess;

			}
			else if(Util.arrayCompare(issuerBuffer, SHORTZERO, HEALTH_BYTES, SHORTZERO, (short) HEALTH_BYTES.length) == 0){
				accessibleFields = canHealthAccess;

			}
			else if(Util.arrayCompare(issuerBuffer, SHORTZERO, DEFAULT_BYTES, SHORTZERO, (short) DEFAULT_BYTES.length) == 0){
				accessibleFields = canDefaultAccess;
			}
			
		// step 4.6
			System.out.println("4.6");
			byte requestAccepted = 0x01;
			for (int i = 0; i < queryLen; i++) {
				byte requestedData = bigStorage[i];
				byte isRequestedDataAvailable = 0x00;
				for (byte b : accessibleFields) {
					if (b == requestedData) {
						isRequestedDataAvailable = 0x01;
					}
				}
				if (isRequestedDataAvailable == 0x00 && requestedData != (byte) 0x00) {
					requestAccepted = 0x00;
					ISOException.throwIt(SW_WRONG_REQUEST);
				}
			}
			System.out.println(requestAccepted);
			if (requestAccepted != 0x01) {
				ISOException.throwIt(SW_WRONG_REQUEST);
				
			}
			else{
		// step 4.7
				System.out.println("4.7");
				Util.arrayCopy(userUniqueKey, SHORTZERO, dataFornym, SHORTZERO, SIZE_OF_UNIQUE_KEY);
				Util.arrayCopy(subjectBuffer, SHORTZERO, dataFornym, SIZE_OF_UNIQUE_KEY, SUBJECT_LEN);

				javacard.security.MessageDigest md = javacard.security.MessageDigest.getInstance(javacard.security.MessageDigest.ALG_SHA_256, false);
				md.reset();

				md.doFinal(dataFornym, (short) 0,(short)dataFornym.length, nym, (short) 0);

		// step 4.8
				//responseToAttributesReleasing
				short offset = 0;
				for (int i = 0; i < queryLen; i++) {
					byte requestedData = bigStorage[i];
					switch (requestedData) {
					// step 4.9
					case NYM_INDEX:
						Util.arrayCopy(nym, SHORTZERO, responseToAttributesReleasing, offset, (short) nym.length);
						offset += nym.length;
						break;
					case NAME_INDEX:
						Util.arrayCopy(name, SHORTZERO, responseToAttributesReleasing, offset, (short) name.length);
						offset += name.length;

						break;
					case ADDRESS_INDEX:
						Util.arrayCopy(address, SHORTZERO, responseToAttributesReleasing, offset, (short) address.length);
						offset += address.length;

						break;
					case COUNTRY_INDEX:
						Util.arrayCopy(country, SHORTZERO, responseToAttributesReleasing, offset, (short) country.length);
						offset += country.length;

						break;
					case BIRTHDATE_INDEX:
						Util.arrayCopy(birthDate, SHORTZERO, responseToAttributesReleasing, offset, (short) birthDate.length);
						offset += birthDate.length;

						break;
					case DONOR_INDEX:
						responseToAttributesReleasing[offset] = donor;
						offset += 1;
						break;
					case AGE_INDEX:
						responseToAttributesReleasing[offset] = age;
						offset += 1;

						break;
					case GENDER_INDEX:
						responseToAttributesReleasing[offset] = gender;
						offset += 1;

						break;
					case PICTURE_INDEX:
						Util.arrayCopy(picture, SHORTZERO, responseToAttributesReleasing, offset, (short) picture.length);
						offset += picture.length;

						break;

					default:
						System.out.println("ARGUMENT NON AVAILABLE");
						break;
					}

				}
		// step 4.10

				javacardx.crypto.Cipher cipher = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
				cipher.init(symKey, javacardx.crypto.Cipher.MODE_ENCRYPT, ivBytes, (short)0, (short)ivBytes.length);


				//		    System.out.println("Challenge: " + javax.xml.bind.DatatypeConverter.printHexBinary(challenge));


				Util.arrayCopy(responseToAttributesReleasing, SHORTZERO, paddedResponseToAttributesReleasing, SHORTZERO,(short) responseToAttributesReleasing.length);
				cipher.doFinal(paddedResponseToAttributesReleasing, (short) 0, (short) paddedResponseToAttributesReleasing.length, encryptedPaddedResponseToAttributesReleasing, (short) 0);
				System.out.println(javax.xml.bind.DatatypeConverter.printHexBinary(encryptedPaddedResponseToAttributesReleasing));
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)encryptedPaddedResponseToAttributesReleasing.length);
				apdu.sendBytesLong(encryptedPaddedResponseToAttributesReleasing,SHORTZERO,(short)encryptedPaddedResponseToAttributesReleasing.length);
			}
		}
		
		
		
	}

	private void auth_card(APDU apdu) {
		System.out.println("STEP 3\n");
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, aesEncryptBytes,(short) 0, SIZE_OF_AES);
		// step 3.4
			if (auth == (byte)0x00) {
				ISOException.throwIt(SW_SP_NOT_AUTH);
			}
			else{
		// step 3.5
			    javacardx.crypto.Cipher cipher = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
			    
				cipher.init(symKey, javacardx.crypto.Cipher.MODE_DECRYPT, ivBytes, (short)0, (short)ivBytes.length);
			    
			    cipher.doFinal(aesEncryptBytes, (short) 0, (short) aesEncryptBytes.length, paddedChallenge, (short) 0);

		// step 3.6
			    Util.arrayCopy(paddedChallenge, SHORTZERO, challenge, SHORTZERO, (short) challenge.length); 
		        javacard.security.Signature signEngine = javacard.security.Signature.getInstance(javacard.security.Signature.ALG_RSA_SHA_PKCS1, false);

		        javacard.security.RSAPrivateKey javacardPrivateKey = (javacard.security.RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_512, false);

		        javacardPrivateKey.setExponent(javacardPrivateExponent, (short) 0, (short) javacardPrivateExponent.length);
				javacardPrivateKey.setModulus(javacardModulus, (short) 0, (short) javacardModulus.length);

		        signEngine.init( javacardPrivateKey, javacard.security.Signature.MODE_SIGN);
				
				javacard.security.MessageDigest md = javacard.security.MessageDigest.getInstance(javacard.security.MessageDigest.ALG_SHA_256, false);
				md.reset();
				
				Util.arrayCopy(challenge, (short) 0, concatChallengeAuth, (short) 0, SIZE_OF_CHALLENGE);
				Util.arrayCopy(authText, (short) 0, concatChallengeAuth, SIZE_OF_CHALLENGE, SIZE_OF_AUTH);
				
				
				md.doFinal(concatChallengeAuth, (short) 0,(short)concatChallengeAuth.length, hashedArray, (short) 0);
				
				signEngine.sign(hashedArray, (short) 0, (short)hashedArray.length, signatureBytes, (short)0);
				
		// step 3.7
				Util.arrayCopy(javacardCert, SHORTZERO, certificateAndSignature, SHORTZERO, SIZE_OF_CERT);
				Util.arrayCopy(signatureBytes, SHORTZERO, certificateAndSignature, SIZE_OF_CERT, SIGN_LEN);
				
				
			    javacardx.crypto.Cipher encryptCipher = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
			    encryptCipher.init(symKey, javacardx.crypto.Cipher.MODE_ENCRYPT, ivBytes, (short)0, (short)ivBytes.length);
			    				

				Util.arrayCopy(certificateAndSignature,(short) 0, paddedDataToEncrypt,(short) 0, (short)certificateAndSignature.length);
				Util.arrayFillNonAtomic(paddedDataToEncrypt, (short)certificateAndSignature.length,(short) (paddedDataToEncrypt.length-certificateAndSignature.length), (byte) 0);

				
				encryptCipher.doFinal(paddedDataToEncrypt, (short) 0, (short) paddedDataToEncrypt.length, encryptedCertificateAndSignature, (short) 0);

				symKey.getKey(keybytes, SHORTZERO);
				
	  // step 3.8

				apdu.setOutgoing();
				apdu.setOutgoingLength((short)encryptedCertificateAndSignature.length);
				apdu.sendBytesLong(encryptedCertificateAndSignature,SHORTZERO,(short)encryptedCertificateAndSignature.length);
			}
		
		
	}

	private void end_auth(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		Util.arrayCopy(buffer, (short)ISO7816.OFFSET_CDATA, fourBytes, (short)0, (short)4);
		short size = (short) bytesToInt(fourBytes, 0);
		System.out.println();
        System.out.println("WE TRYIN TO DELETE NEWS HERE BITCH: " + size);
		Util.arrayCopy(buffer, (short) (ISO7816.OFFSET_CDATA + 4), lenAndEncryptedPaddedChallenge,(short) 0, size);

		javacardx.crypto.Cipher cipher = javacardx.crypto.Cipher.getInstance(javacardx.crypto.Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);

	    cipher.init(symKey, javacardx.crypto.Cipher.MODE_DECRYPT, ivBytes, (short)0, (short)ivBytes.length);
		
//		System.out.println(symetricKey.getEncoded());
//		try {
//			aesdec.init(Cipher.DECRYPT_MODE, symetricKey, iv);
//		} catch (InvalidKeyException e) {
//			System.out.println("InvalidKeyException");
//		} catch (InvalidAlgorithmParameterException e) {
//			System.out.println("InvalidAlgorithmParameterException");
//		}
		
		
		Util.arrayCopy(lenAndEncryptedPaddedChallenge, (short) 0, aesEncryptBytes, (short) 0, (short)aesEncryptBytes.length);
		
		cipher.doFinal(aesEncryptBytes, (short)0,(short) aesEncryptBytes.length,lenAndEncryptedPaddedChallenge,(short) 0);
		
		
		int responseShort =  (lenAndEncryptedPaddedChallenge[0] << 8 | (lenAndEncryptedPaddedChallenge[1] & 0xFF));
		short previousChallenge = (short)  (challenge[0] << 8 | (challenge[1] & 0xFF));
		if (responseShort != previousChallenge+1) {
			ISOException.throwIt(SW_WRONG_CHALLENGE);
		}
		auth = (byte) 0x01;
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)0);
		apdu.sendBytesLong(emptyResponse,(short)0,(short)emptyResponse.length);
		
	}
	
	private static int bytesToInt(byte[] bytes, int offset){
		return bytes[offset] << 24 | (bytes[offset+1] & 0xFF) << 16 | (bytes[offset+2] & 0xFF) << 8 | (bytes[offset+3] & 0xFF);
	}

	private void echo(APDU apdu){
		System.out.println("Echo");
		byte[] buffer = apdu.getBuffer();
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short START = 0;
		Util.arrayCopy(buffer, START, storage, START, (short)8);
		short readCount = apdu.setIncomingAndReceive();
		short i = ISO7816.OFFSET_CDATA;
		while ( bytesLeft > 0){
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, storage, i, readCount);
			bytesLeft -= readCount;
			i+=readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)storage.length);
		apdu.sendBytesLong(storage,(short)0,(short)storage.length);
		
		 
	}
	

	
	private void sign_data(APDU apdu) {
		if(!pin.isValidated()){
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		}
		else{
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)serial.length);
			apdu.sendBytesLong(serial,(short)0,(short)serial.length);
			
		}
		
	}

	/*
	 * This method is used to authenticate the owner of the card using a PIN code.
	 */
	private void validatePIN(APDU apdu){
		byte[] buffer = apdu.getBuffer();
		//The input data needs to be of length 'PIN_SIZE'.
		//Note that the byte values in the Lc and Le fields represent values between
		//0 and 255. Therefore, if a short representation is required, the following
		//code needs to be used: short Lc = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		if(buffer[ISO7816.OFFSET_LC]==PIN_SIZE){
			//This method is used to copy the incoming data in the APDU buffer.
			apdu.setIncomingAndReceive();
			//Note that the incoming APDU data size may be bigger than the APDU buffer 
			//size and may, therefore, need to be read in portions by the applet. 
			//Most recent smart cards, however, have buffers that can contain the maximum
			//data size. This can be found in the smart card specifications.
			//If the buffer is not large enough, the following method can be used:
			//
			//byte[] buffer = apdu.getBuffer();
			//short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
			//Util.arrayCopy(buffer, START, storage, START, (short)5);
			//short readCount = apdu.setIncomingAndReceive();
			//short i = ISO7816.OFFSET_CDATA;
			//while ( bytesLeft > 0){
			//	Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, storage, i, readCount);
			//	bytesLeft -= readCount;
			//	i+=readCount;
			//	readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
			//}
			if (pin.check(buffer, ISO7816.OFFSET_CDATA,PIN_SIZE)==false)
				ISOException.throwIt(SW_VERIFICATION_FAILED);
		}else{ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);}
	}
	
	/*
	 * This method checks whether the user is authenticated and sends
	 * the identity file.
	 */
	private void getSerial(APDU apdu){
		//If the pin is not validated, a response APDU with the
		//'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if(!pin.isValidated())ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else{
			//This sequence of three methods sends the data contained in
			//'identityFile' with offset '0' and length 'identityFile.length'
			//to the host application.
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)serial.length);
			apdu.sendBytesLong(serial,(short)0,(short)serial.length);
		}
	}
}
