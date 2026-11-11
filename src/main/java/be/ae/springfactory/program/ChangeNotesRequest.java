package be.ae.springfactory.program;

/** The body of PUT /api/sessions/{id}/notes: {notes}. Null or blank notes clear them. */
record ChangeNotesRequest(String notes) {}
