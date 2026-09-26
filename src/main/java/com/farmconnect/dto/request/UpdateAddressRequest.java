package com.farmconnect.dto.request;

import lombok.Data;

/**
 * Updates the address fields already present on the User entity
 * (address, city, state, pincode). The schema has one address per user,
 * not a separate multi-address table, so this matches the real data model
 * instead of inventing a new one.
 */
@Data
public class UpdateAddressRequest {
    private String address;
    private String city;
    private String state;
    private String pincode;
}
