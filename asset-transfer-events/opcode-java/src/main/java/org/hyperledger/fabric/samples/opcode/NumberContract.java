package org.hyperledger.fabric.samples.opcode;

import java.time.Instant;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
/**
 * Auxiliary Chaincode class.
*
* @see org.hyperledger.fabric.shim.Chaincode
* <p>
* Each chaincode transaction function must take, Context as first parameter.
* Unless specified otherwise via annotation (@Contract or @Transaction), the contract name
* is the class name (without package)
* and the transaction name is the method name.
*/

@Contract(
name = "numbers",
info = @Info(
        title = "Number Events Contract",
        description = "The hyperlegendary note processing events sample",
        version = "0.0.1-SNAPSHOT",
        license = @License(
                name = "Apache 2.0 License",
                url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
        contact = @Contact(
                email = "a.transfer@example.com",
                name = "Fabric Development Team",
                url = "https://hyperledger.example.com")))
@Default
public final class NumberContract implements ContractInterface {
  
    public NumberContract() {
    }
    /**
     * Retrieves the asset details with the specified ID
     *
     * @param ctx     the transaction context
     * @param shipper the shipper
     * @return the new note number
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetNoteNumber(final Context ctx, final String shipper) {
        System.out.printf("GetNoteNumber: ID %s\n", shipper);


        return "shipper" + Instant.now().toEpochMilli();
    }
    
}