var Config = function () {
    this.nflowEndpoints = [
        {
            id: 'Full stack example',
            title: 'Full stack example API',
            apiUrl: '/nflow/api'
        }
    ];

    this.radiator = {
        pollPeriod: 15,
        maxHistorySize: 10000
    };
};
