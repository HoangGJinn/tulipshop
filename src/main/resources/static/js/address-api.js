// Address API using jQuery AJAX
const AddressAPI = {
    baseURL: 'https://provinces.open-api.vn/api/v1/',

    // Get list of provinces
    getListProvinces: function() {
        return $.ajax({
            url: this.baseURL + 'p',
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get province by code
    getProvince: function(code, params = {}) {
        return $.ajax({
            url: this.baseURL + 'p/' + code,
            type: 'GET',
            data: params,
            dataType: 'json'
        });
    },

    // Get list of districts
    getListDistricts: function() {
        return $.ajax({
            url: this.baseURL + 'd',
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get district with depth 2 (includes wards)
    getDistrictDepth2: function(code) {
        return $.ajax({
            url: this.baseURL + 'd/' + code + '?depth=2',
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get list of wards
    getListWards: function() {
        return $.ajax({
            url: this.baseURL + 'w',
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get ward by code
    getWard: function(code) {
        return $.ajax({
            url: this.baseURL + 'w/' + code,
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get districts by province code
    getDistrictsByProvince: function(provinceCode) {
        return $.ajax({
            url: this.baseURL + 'p/' + provinceCode + '?depth=2',
            type: 'GET',
            dataType: 'json'
        });
    },

    // Get wards by district code
    getWardsByDistrict: function(districtCode) {
        return $.ajax({
            url: this.baseURL + 'd/' + districtCode + '?depth=2',
            type: 'GET',
            dataType: 'json'
        });
    }
};

